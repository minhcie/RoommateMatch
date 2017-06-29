package src.main.java;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.sun.jersey.api.client.ClientResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.apache.log4j.Logger;

public class TouchPointUtil {
    private static final Logger log = Logger.getLogger(TouchPointUtil.class.getName());

    public enum AlphaPrograms {
        intake(782),
        outreachDowntown(785),
        outreachHillcrest(783),
        outreachPortSD(784),
        homeFinder(792);

        int value;
        AlphaPrograms(int v) {
            this.value = v;
        }
        public int getValue() {
            return this.value;
        }
    }

    public static int getAlphaProgramId(String name) {
        int programId = 0;
        if (name == null || name.length() <= 0) {
            log.error("Trying to get Alpha Project program ID without program name");
            return programId;
        }

        String programName = name.toLowerCase();
        if (programName.equalsIgnoreCase("intake")) {
            programId = AlphaPrograms.intake.getValue();
            return programId;
        }
        if (programName.equalsIgnoreCase("home-finder")) {
            programId = AlphaPrograms.homeFinder.getValue();
            return programId;
        }
        log.error("Invalid program name: " + name);
        return programId;
    }

    public static void addConsentDocument(EtoAuthentication auth, Long subjectId,
                                          RoommateData data, String consentFileName) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        Calendar cal = Calendar.getInstance();
        if (data.entryDate != null) {
            cal.setTime(data.entryDate);
        }
        Date dt = cal.getTime();
        int intakePrgmId = AlphaPrograms.intake.getValue();

        JSONObject input = new JSONObject();
        input.put("TouchPointID", new Integer(14)); // CIE Client Authorization TP.
        input.put("SubjectID", subjectId);
        input.put("ResponseCreatedDate", "/Date(" + dt.getTime() + ")/");
        input.put("ProgramID", new Integer(intakePrgmId));

        JSONArray respElements = new JSONArray();
        JSONObject ele = new JSONObject();
        ele.put("ElementID", new Integer(137)); // Date authorization recorded.
        ele.put("ElementType", new Integer(9));
        ele.put("Value", sdf.format(dt));
        respElements.add(ele);

        ele = new JSONObject();
        ele.put("ElementID", new Integer(165)); // Date authorization expired.
        ele.put("ElementType", new Integer(9));
        cal.add(Calendar.YEAR, 3);
        dt = cal.getTime();
        ele.put("Value", sdf.format(dt));
        respElements.add(ele);

        // File attachments.
        Path path = Paths.get(consentFileName);
        byte[] fileData = Files.readAllBytes(path);
        JSONObject f = new JSONObject();
        f.put("Caption", null);
        JSONArray fileArr = new JSONArray();
        for (int k = 0; k < fileData.length; k++) {
            int n = fileData[k] & 0xff; // Since bytes are signed in Java.
            fileArr.add(n);
        }
        f.put("FileContent", fileArr);
        f.put("FileName", data.firstName + "_" + data.lastName + "_Authorization.pdf");

        ele = new JSONObject();
        ele.put("ElementID", new Integer(138)); // Authorization attachment.
        ele.put("ElementType", new Integer(29));
        ele.put("ResponseFileAttachment", f);
        ele.put("Value", null);
        respElements.add(ele);

        // Add response elements.
        input.put("ResponseElements", respElements);

        // Wrap request JSON string.
        String jsonStr = input.toString("TouchPointResponse", input);
        String inputStr = "{" + jsonStr + "}";

        // Post request.
        ClientResponse response = EtoServiceUtil.postRequest("https://services.etosoftware.com/API/TouchPoint.svc/TouchPointResponseAdd/",
                                                             auth, inputStr);
        if (response.getStatus() != 200) {
            log.error(response.toString());
        }
        else {
            // Parse response.
            String resp = response.getEntity(String.class);
            log.info("Client Authorization - Response from server:");
            log.info(resp);
        }
    }

    public static void addProgramEnrollment(Connection sqlConn, EtoAuthentication auth,
                                         DbClient client, String programName,
                                         Date entryDate) throws Exception {
        log.info("Adding new program enrollment (" + programName + ") in ETO...");
        int programId = getAlphaProgramId(programName);
        if (programId == 0) {
            return;
        }

        JSONObject input = new JSONObject();
        input.put("participantID", client.etoParticipantSiteId);
        input.put("programID", new Integer(programId));
        long prgmStartDate = 0;
        Date now = new Date();
        if (entryDate != null) {
            prgmStartDate = entryDate.getTime();
        }
        else {
            prgmStartDate = now.getTime();
        }
        input.put("startDate", "/Date(" + prgmStartDate + ")/");
        input.put("projectedEndDate", null);

        // @debug.
        log.info(input.toString());

        // Post request.
        ClientResponse response = EtoServiceUtil.postRequest("https://services.etosoftware.com/API/Actor.svc/participant/enrollment/",
                                                             auth, input.toString());
        if (response.getStatus() != 200) {
            log.error(response.toString());
        }
        else {
            // Parse response.
            Long enrollmentId = EtoServiceUtil.parseResponse(response, "SaveParticipantEnrollmentResult", "ID");
            log.info("Client enrollment ID: " + enrollmentId);

            // Find matching CIE program using ETO's program id.
            DbProgram prgm = DbProgram.findByEtoProgramId(sqlConn, programId);
            if (prgm == null) {
                log.error("CIE Program not found while trying to add new program enrollment");
                return;
            }

            // Save enrollment info in CIE database.
            log.info("Adding new program enrollment in CIE...");
            DbEnrollment enroll = new DbEnrollment();
            enroll.clientId = client.id;
            enroll.programId = prgm.id;
            if (entryDate != null) {
                enroll.startDate = entryDate;
            }
            else {
                enroll.startDate = now;
            }
            enroll.endDate = null;
            enroll.dismissalReasonId = 1; // Completed.
            enroll.insert(sqlConn);
        }
    }

    public static void addRoommateData(Connection sqlConn, EtoAuthentication auth,
                                       DbClient client, RoommateData data) throws Exception {
        log.info("Adding roommate matching criteria...");
        int programId = getAlphaProgramId("home-finder");
        if (programId == 0) {
            return;
        }

        // Update ETO session's current program before populating TouchPoint.
        EtoServiceUtil.setCurrentProgram(programId, auth);

        // ETO Home Finder TouchPoint.
        JSONObject input = new JSONObject();
        input.put("TouchPointID", new Integer(54));
        input.put("SubjectID", client.etoSubjectId);
        Date now = new Date();
        input.put("ResponseCreatedDate", "/Date(" + now.getTime() + ")/");
        input.put("ProgramID", new Integer(programId));

        // TouchPoint response elements.
        JSONArray respElements = new JSONArray();
        JSONObject ele = new JSONObject();

        // Question (element) response choices.
        JSONArray respElementChoices = new JSONArray();
        JSONObject choice = new JSONObject();

        // Rental assistance subsidy.
        respElements.add(createYesNoElement(data.rentalSubsidy, 814, 1277, 1278));

        if (data.rentalSubsidy) {
            boolean psh = false;
            if (data.subsidyType.length() > 0 && data.subsidyType.equalsIgnoreCase("PSH")) {
                psh = true;
            }

            // Subsidy type.
            respElements.add(createYesNoElement(psh, 815, 1279, 1280));

            // Subsidy source.
            ele = new JSONObject();
            ele.put("ElementType", new Integer(4));
            choice = new JSONObject();

            if (psh) {
                ele.put("ElementID", new Integer(816));
                switch (data.pshSource) {
                    case "CoC":
                        choice.put("TouchPointElementChoiceID", new Integer(1281));
                        break;
                    case "HUD VASH":
                        choice.put("TouchPointElementChoiceID", new Integer(1282));
                        break;
                    case "MHSA":
                        choice.put("TouchPointElementChoiceID", new Integer(1283));
                        break;
                    case "Section 8":
                        choice.put("TouchPointElementChoiceID", new Integer(1284));
                        break;
                    default:
                        choice.put("TouchPointElementChoiceID", new Integer(1285));
                        break;
                }
            }
            else {
                ele.put("ElementID", new Integer(817));
                switch (data.rrhSource) {
                    case "CoC":
                        choice.put("TouchPointElementChoiceID", new Integer(1286));
                        break;
                    case "ESG":
                        choice.put("TouchPointElementChoiceID", new Integer(1287));
                        break;
                    case "SSVF":
                        choice.put("TouchPointElementChoiceID", new Integer(1288));
                        break;
                    default:
                        choice.put("TouchPointElementChoiceID", new Integer(1289));
                        break;
                }
            }

            respElementChoices = new JSONArray();
            respElementChoices.add(choice);
            ele.put("ResponseElementChoices", respElementChoices);
            respElements.add(ele);
        }

        // Have current monthly income?
        if (data.monthlyIncomeAmount.length() > 0 || data.incomeSource.length() > 0) {
            respElements.add(createYesNoElement(true, 818, 1290, 1291));
        }

        // Monthly income.
        ele = new JSONObject();
        ele.put("ElementID", new Integer(819));
        ele.put("ElementType", new Integer(5));
        ele.put("Value", data.monthlyIncomeAmount);
        respElements.add(ele);

        // Income source.
        ele = new JSONObject();
        ele.put("ElementID", new Integer(820));
        ele.put("ElementType", new Integer(5));
        ele.put("Value", data.incomeSource);
        respElements.add(ele);

        respElements.add(createYesNoElement(data.cleanupAfterThemselves, 821, 1292, 1293));
        respElements.add(createYesNoElement(data.quietBy10pm, 822, 1294, 1295));
        respElements.add(createYesNoElement(data.likeToTalk, 823, 1296, 1297));
        respElements.add(createYesNoElement(data.keepToThemselves, 824, 1298, 1299));
        respElements.add(createYesNoElement(data.noPet, 825, 1300, 1301));
        respElements.add(createYesNoElement(data.noOvernightGuest, 826, 1302, 1303));
        respElements.add(createYesNoElement(data.noSmoke, 827, 1304, 1305));
        respElements.add(createYesNoElement(data.noAlcohol, 828, 1306, 1307));
        respElements.add(createYesNoElement(data.noMarijuana, 829, 1308, 1309));
        respElements.add(createYesNoElement(data.noIllegalSubstance, 830, 1310, 1311));

        // Add response elements.
        input.put("ResponseElements", respElements);

        // Wrap request JSON string.
        String jsonStr = input.toString("TouchPointResponse", input);
        String inputStr = "{" + jsonStr + "}";

        // @debug.
        log.info(inputStr);

        // Post request.
        ClientResponse response = EtoServiceUtil.postRequest("https://services.etosoftware.com/API/TouchPoint.svc/TouchPointResponseAdd/",
                                                             auth, inputStr);
        if (response.getStatus() != 200) {
            log.error(response.toString());
        }
        else {
            // Parse response.
            Long respId = EtoServiceUtil.parseResponse(response, "AddTouchPointResponseResult",
                                                       "TouchPointResponseID");
            log.info("Home Finder/Roommate Matching response ID: " + respId);
        }

        // Upsert roommate matching criteria in CIE database.
        DbRoommate roommate = DbRoommate.findByClientId(sqlConn, client.id);
        if (roommate == null) {
            roommate = new DbRoommate();
        }
        roommate.clientId = client.id;
        roommate.rentalSubsidy = data.rentalSubsidy;
        roommate.subsidyType = data.subsidyType;
        roommate.pshSource = data.pshSource;
        roommate.rrhSource = data.rrhSource;
        roommate.incomeSource = data.incomeSource;
        roommate.monthlyIncomeAmount = data.monthlyIncomeAmount;
        roommate.cleanupAfterThemselves = data.cleanupAfterThemselves;
        roommate.quietBy10pm = data.quietBy10pm;
        roommate.likeToTalk = data.likeToTalk;
        roommate.keepToThemselves = data.keepToThemselves;
        roommate.noPet = data.noPet;
        roommate.noOvernightGuest = data.noOvernightGuest;
        roommate.noSmoke = data.noSmoke;
        roommate.noAlcohol = data.noAlcohol;
        roommate.noMarijuana = data.noMarijuana;
        roommate.noIllegalSubstance = data.noIllegalSubstance;

        if (roommate.id == 0) {
            log.info("Inserting new roommate record...");
            roommate.insert(sqlConn);
        }
        else {
            log.info("Updating existing roommate record (id: " + roommate.id + ")...");
            roommate.update(sqlConn);
        }
    }

    private static JSONObject createYesNoElement(boolean isYes, int elementId,
                                                 int yesChoiceId, int noChoiceId) {
        JSONObject ele = new JSONObject();
        ele.put("ElementID", new Integer(elementId));
        ele.put("ElementType", new Integer(4));

        JSONObject choice = new JSONObject();
        if (isYes) {
            choice.put("TouchPointElementChoiceID", new Integer(yesChoiceId));
        }
        else {
            choice.put("TouchPointElementChoiceID", new Integer(noChoiceId));
        }

        JSONArray respElementChoices = new JSONArray();
        respElementChoices.add(choice);
        ele.put("ResponseElementChoices", respElementChoices);
        return ele;
    }
}
