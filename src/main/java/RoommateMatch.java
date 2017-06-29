package src.main.java;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.PdfContentByte;;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfWriter;

import com.sun.jersey.api.client.ClientResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

public class RoommateMatch {

    private static final Logger log = Logger.getLogger(RoommateMatch.class.getName());
    private static final long ALPHA_PROJECT_ORG_ID = 11;
    private static final String CIE_CONSENT_TEMPLATE = "CIE_Consent_Template.pdf";

    public static void main(String[] args) {
        Connection conn = null;

        try {
            // Establish DB connection.
            conn = DbUtil.getDBConnection();
            if (conn == null) {
                System.exit(-1);
            }

            // ETO Authenticate/Sign-on to Alpha Project.
            EtoAuthentication auth = EtoServiceUtil.authenticate();
            if (auth == null) {
                System.exit(-1);
            }

            // Read client data.
            List<RoommateData> roommateList = XcelUtil.readData();
            if (roommateList == null || roommateList.size() <= 0) {
                System.exit(-1);
            }
            log.info("Total " + roommateList.size() + " clients found...\n");

            // Load data into both ETO and CIE.
            for (int i = 0; i < roommateList.size(); i++) {
                RoommateData data = roommateList.get(i);
                addRoommateData(conn, auth, data);
            }

            // Calculate matching score for each client in the list.
            for (int i = 0; i < roommateList.size(); i++) {
                RoommateData data = roommateList.get(i);
                calculateMatchingScores(conn, data);
            }
        }
        catch (IOException ioe) {
            log.error(ioe.getMessage());
            ioe.printStackTrace();
        }
        catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
        finally {
            DbUtil.closeConnection(conn);
        }
    }

    private static void addRoommateData(Connection conn, EtoAuthentication auth,
                                        RoommateData data) throws Exception {
        String caseNumber = "ALPH_" + data.clientUid;
        log.info("Finding client case number: " + caseNumber + "...");
        DbClient client = DbClient.findByCaseNumber(conn, ALPHA_PROJECT_ORG_ID, caseNumber);
        if (client == null) {
            // Add new client in ETO.
            log.info("Client not found, adding new client (" + data.firstName + " " + data.lastName + ")...");

            // Standard demographics.
            JSONObject input = new JSONObject();
            input.put("FirstName", data.firstName);
            input.put("LastName", data.lastName);

            long genderId = 6; // Unknown.
            if (data.gender.equalsIgnoreCase("Female")) {
                input.put("Gender", new Integer(1));
                genderId = 1;
            }
            else if (data.gender.equalsIgnoreCase("Male")) {
                input.put("Gender", new Integer(0));
                genderId = 2;
            }
            else {
                input.put("Gender", new Integer(8)); // Data not collected.
            }

            if (data.dob != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(data.dob);
                cal.set(Calendar.HOUR, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                long dob = cal.getTime().getTime();
                input.put("DateOfBirth", "/Date(" + dob + ")/");
            }

            String ssn = "";
            if (data.ssn != null) {
                ssn = data.ssn.replaceAll("-", "");
                input.put("SocialSecurityNumber", ssn);
            }
            input.put("CaseNumber", data.clientUid);

            // Custom demographics.
            JSONArray custList = new JSONArray();
            JSONObject cust = new JSONObject();
            cust.put("CDID", new Integer(3758)); // Gender (HUD).
            cust.put("CharacteristicType", new Integer(4));
            if (data.gender.equalsIgnoreCase("Female")) {
                cust.put("value", new Integer(8952));
            }
            else if (data.gender.equalsIgnoreCase("Male")) {
                cust.put("value", new Integer(8953));
            }
            else {
                cust.put("value", new Integer(8959)); // Data not collected.
            }
            custList.add(cust);

            // Name data quality (HUD).
            cust = new JSONObject();
            cust.put("CDID", new Integer(3762));
            cust.put("CharacteristicType", new Integer(4));
            cust.put("value", new Integer(8970));
            custList.add(cust);

            // SSN data quality (HUD).
            cust = new JSONObject();
            cust.put("CDID", new Integer(3764));
            cust.put("CharacteristicType", new Integer(4));
            if (data.ssn != null && data.ssn.length() > 0) {
                cust.put("value", new Integer(8983));
            }
            else {
                cust.put("value", new Integer(8987));
            }
            custList.add(cust);

            // DOB data quality (HUD).
            cust = new JSONObject();
            cust.put("CDID", new Integer(3760));
            cust.put("CharacteristicType", new Integer(4));
            cust.put("value", new Integer(8965));
            custList.add(cust);

            // Ethnicity (HUD).
            int ethnicityId = 0; // Should match id from data_quality_choice table.
            String ethnicity = "";
            if (data.ethnicity != null) {
                ethnicity = data.ethnicity.toLowerCase();
            }
            cust = new JSONObject();
            cust.put("CDID", new Integer(3759));
            cust.put("CharacteristicType", new Integer(4));
            if (ethnicity.contains("non-hispanic") || ethnicity.contains("non-latino")) {
                cust.put("value", new Integer(8960));
                ethnicityId = 16;
            }
            else if (ethnicity.contains("hispanic") || ethnicity.contains("latino")) {
                cust.put("value", new Integer(8961));
                ethnicityId = 17;
            }
            else {
                cust.put("value", new Integer(8964));
                ethnicityId = 20; // Not collected.
            }
            custList.add(cust);

            // Race (HUD).
            long raceId = 0; // Should match id from race table.
            String race = "";
            if (data.race != null) {
                race = data.race.toLowerCase();
            }
            cust = new JSONObject();
            cust.put("CDID", new Integer(3763));
            cust.put("CharacteristicType", new Integer(5));
            if (race.contains("caucasian") || race.contains("white")) {
                cust.put("value", new Integer(8979));
                raceId = 11;
            }
            else if (race.contains("hispanic") || race.contains("latino") ||
                     race.contains("other race")) {
                cust.put("value", new Integer(8982));
                raceId = 12;
            }
            else if (race.contains("african american") || race.contains("black")) {
                cust.put("value", new Integer(8977));
                raceId = 13;
            }
            else if (race.contains("asian")) {
                cust.put("value", new Integer(8976));
                raceId = 14;
            }
            else {
                cust.put("value", new Integer(8982));
                raceId = 17; // Unknown.
            }
            custList.add(cust);

            // Veteran status (HUD).
            int vetStatus = 21; // No (id from data_quality_choice table).
            cust = new JSONObject();
            cust.put("CDID", new Integer(3765));
            cust.put("CharacteristicType", new Integer(4));
            if (data.veteran) {
                cust.put("value", new Integer(8989));
                vetStatus = 22;
            }
            else {
                cust.put("value", new Integer(8988));
            }
            custList.add(cust);

            // Include in standard demographics.
            input.put("CustomDemoData", custList);

            // Wrap request JSON string.
            String jsonStr = input.toString("participant", input);
            String inputStr = "{" + jsonStr + "}";

            // @debug.
            //log.info(inputStr);

            // Post request to add client to ETO.
            ClientResponse response = EtoServiceUtil.postRequest("https://services.etosoftware.com/API/Actor.svc/participant/",
                                                                 auth, inputStr);
            if (response.getStatus() != 200) {
                log.error(response.toString());
                return;
            }

            // Parse response.
            String resp = response.getEntity(String.class);
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(resp);
            JSONObject jsonObj = (JSONObject)obj;

            JSONObject result = (JSONObject)jsonObj.get("SaveParticipantResult");
            String guid = (String)result.get("GUID");
            log.info("Enterprise id: " + guid);
            Long participantId = (Long)result.get("ID");
            log.info("Participant id: " + participantId);
            Long subjectId = (Long)result.get("SubjectID");
            log.info("Subject id: " + subjectId);

            // Create CIE client consent document.  Note that in order to search
            // for client in CIE, you will need to attach consent document.
            String consentFileName = createConsentDocument(data);

            // Attach consent document to the new client.
            TouchPointUtil.addConsentDocument(auth, subjectId, data, consentFileName);

            // Save consent document.
            log.info("Archiving CIE consent document (" + consentFileName + ")...");
            String workingDir = System.getProperty("user.dir");
            String archiveDir = workingDir + "\\archive\\";
            File f = new File(consentFileName);
            f.renameTo(new File(archiveDir + f.getName()));

            // Save new client in CIE database.
            client = new DbClient();
            client.organizationId = ALPHA_PROJECT_ORG_ID;
            client.genderId = genderId;
            client.firstName = data.firstName;
            client.lastName = data.lastName;
            client.dob = data.dob;
            client.ssn = ssn;
            client.caseNumber = caseNumber;
            client.ethnicity = ethnicityId;
            client.etoEnterpriseId = UUID.fromString(guid);
            client.etoParticipantSiteId = participantId;
            client.etoSubjectId = subjectId;
            client.insert(conn);

            // Also save client race.
            DbClientRace dbRace = new DbClientRace();
            dbRace.clientId = client.id;
            dbRace.raceId = raceId;
            dbRace.insert(conn);

            // Enroll new client in the default intake program.
            TouchPointUtil.addProgramEnrollment(conn, auth, client, "intake",
                                                data.entryDate);

            // Also enroll in the home finder program.
            TouchPointUtil.addProgramEnrollment(conn, auth, client, "home-finder",
                                                data.entryDate);
        }

        // Add roommate data into ETO.
        TouchPointUtil.addRoommateData(conn, auth, client, data);
    }

    private static String createConsentDocument(RoommateData data) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        SimpleDateFormat sdf2 = new SimpleDateFormat("MMddyyyy");

        // Consent filename.
        String consentFileName = data.firstName + "_" + data.lastName + ".pdf";
    	log.info("Creating CIE consent document (" + consentFileName + ")...");

        try {
            // Create pdf document writer.
            String name = data.firstName + " " + data.lastName;
            String dob = "";
            if (data.dob != null) {
                dob = sdf.format(data.dob);
            }
            String consentDate = "";
            if (data.entryDate != null) {
                consentDate = sdf.format(data.entryDate);
            }
            else {
                Date now = new Date();
                consentDate = sdf.format(now);
            }

            Document doc = new Document();
            PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(consentFileName));
            doc.open();
            PdfContentByte cb = writer.getDirectContent();
            ByteArrayOutputStream stampedBuffer = null;

            // Read pdf template.
            log.info("Reading PDF template (" + CIE_CONSENT_TEMPLATE + ")...");
            int pages = 2;
            for (int i = 1; i <= pages; i++) {
                PdfReader templateReader = new PdfReader(new FileInputStream(CIE_CONSENT_TEMPLATE));

                // Start the process of adding extra content to existing PDF.
                stampedBuffer = new ByteArrayOutputStream();
                PdfStamper stamper = new PdfStamper(templateReader, stampedBuffer);
                stamper.setFormFlattening(true);

                // Getting template fields.
                AcroFields form = stamper.getAcroFields();

                // Fill template with data.
                if (i == 1) {
                    form.setField("ClientName1", name);
                    form.setField("DoB", dob);
                    form.setField("Gender", data.gender);
                    form.setField("Address", "");
                }
                else {
                    form.setField("ConsentFileName", consentFileName);
                    form.setField("ClientName2", name);
                    form.setField("ConsentDate", consentDate);

                    // Page break.
                    doc.newPage();
                }

                // Close template reader, clean up.
                stamper.close();
                templateReader.close();
                form = null;

                // Import page content.
                PdfReader stampedReader = new PdfReader(stampedBuffer.toByteArray());
                PdfImportedPage page = writer.getImportedPage(stampedReader, i);
                cb.addTemplate(page, 0, 0);
            }

            // Close document writer.
            doc.close();
            writer.close();

            // Create output file.
            new File(consentFileName);
        }
        catch (IOException ioe) {
            log.error(ioe.getMessage());
            ioe.printStackTrace();
        }
        catch (DocumentException de) {
            log.error(de.getMessage());
            de.printStackTrace();
        }
        catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
        return consentFileName;
    }

    private static void calculateMatchingScores(Connection conn, RoommateData data) throws Exception {
        String clientName = data.firstName + " " + data.lastName;
        log.info("Calculating roommate matching score for " + clientName);
        List<DbRoommate> results = DbRoommate.findAll(conn);
        if (results == null || results.size() <= 0) {
            log.info("No records found in CIE database");
            return;
        }

        // Roommate matching score list.
        List<RoommateScore> roommateScores = new ArrayList<RoommateScore>();

        // Calculate matching scores for each client in the list.
        for (int i = 0; i < results.size(); i++) {
            DbRoommate r = results.get(i);

            // Skip record that is the same as matching record.
            String matchingCaseNumber = "ALPH_" + data.clientUid;
            //log.debug("r.caseNumber: " + r.caseNumber + ", matchingCaseNumber: " + matchingCaseNumber);
            if (r.caseNumber.equalsIgnoreCase(matchingCaseNumber)) {
                //log.debug("same client... skip");
                continue;
            }

            log.debug("roommate name: " + r.firstName + " " + r.lastName);
            RoommateScore rs = new RoommateScore(r.firstName, r.lastName);

            if (r.cleanupAfterThemselves == data.cleanupAfterThemselves) {
                //log.debug("cleanupAfterThemselves true");
                rs.totalScore += 1;
                rs.socialScore += 1;
            }
            if (r.quietBy10pm == data.quietBy10pm) {
                //log.debug("quietBy10pm true");
                rs.totalScore += 1;
                rs.socialScore += 1;
            }
            if (r.likeToTalk == data.likeToTalk) {
                //log.debug("likeToTalk true");
                rs.totalScore += 1;
                rs.socialScore += 1;
            }
            if (r.keepToThemselves == data.keepToThemselves) {
                //log.debug("keepToThemselves true");
                rs.totalScore += 1;
                rs.socialScore += 1;
            }
            if (r.noPet == data.noPet) {
                //log.debug("noPet true");
                rs.totalScore += 1;
                rs.socialScore += 1;
            }
            if (r.noOvernightGuest == data.noOvernightGuest) {
                //log.debug("noOvernightGuest true");
                rs.totalScore += 1;
                rs.socialScore += 1;
            }
            if (r.noSmoke == data.noSmoke) {
                //log.debug("noSmoke true");
                rs.totalScore += 1;
                rs.socialScore += 1;
            }
            if (r.noAlcohol == data.noAlcohol) {
                //log.debug("noAlcohol true");
                rs.totalScore += 1;
                rs.substanceScore += 1;
            }
            if (r.noMarijuana == data.noMarijuana) {
                //log.debug("noMarijuana true");
                rs.totalScore += 1;
                rs.substanceScore += 1;
            }
            if (r.noIllegalSubstance == data.noIllegalSubstance) {
                //log.debug("noIllegalSubstance true");
                rs.totalScore += 1;
                rs.substanceScore += 1;
            }

            log.debug("total score: " + rs.totalScore);
            log.debug("social score: " + rs.socialScore);
            log.debug("substance score: " + rs.substanceScore);
            if (rs.totalScore > 0) {
                roommateScores.add(rs);
            }
        }

        // Sort roommate matching score list with highest total score on top.
        Collections.sort(roommateScores);

        // Send email notification to generic case manager.
        int etoProgramId = TouchPointUtil.getAlphaProgramId("home-finder");
        DbGenericCaseManager caseMgr = DbGenericCaseManager.findByProgram(conn, etoProgramId);
        if (caseMgr != null) {
            sendEmail(caseMgr.email, clientName, roommateScores);
            //sendEmail("mtran@211sandiego.org", clientName, roommateScores);
        }
        else {
            log.info("Generic case manager not found!");
        }
    }

    private static void sendEmail(String mgrEmailAddr, String clientName,
                                  List<RoommateScore> scores) throws Exception {
        // Format email body.
        StringBuffer msg = new StringBuffer();
        msg.append("<br>");
        msg.append("<i>CIE-Alpha Project Home Finder Program Auto Notification</i>");
        msg.append("<br>");
        msg.append("Client Name: <b>" + clientName + "</b>");
        msg.append("<br><br>");
        msg.append("<table style='width:100%'>");
        msg.append("<tr>");
        msg.append("<th>Roommate Name</th>");
        msg.append("<th>Total Score</th>");
        msg.append("<th>Social Score</th>");
        msg.append("<th>Substance Score</th>");
        msg.append("</tr>");
        for (int i = 0; i < scores.size(); i++) {
            RoommateScore rs = scores.get(i);
            String name = rs.firstName + " " + rs.lastName.charAt(0);
            msg.append("<tr>");
            msg.append("<td>" + name + "</td>");
            msg.append("<td align='center'>" + rs.totalScore + "</td>");
            msg.append("<td align='center'>" + rs.socialScore + "</td>");
            msg.append("<td align='center'>" + rs.substanceScore + "</td>");
            msg.append("</tr>");
        }
        msg.append("</table>");
        msg.append("<br>");

        // Config email properties (Non-SSL).
        Properties props = new Properties();
        props.put("mail.smtp.host", "mail.211sandiego.org");
        props.put("mail.smtp.auth", "true");
        props.put("mail.debug", "true");
        //props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.enable", "false");
        props.put("mail.smtp.port", "25");
        props.put("mail.smtp.socketFactory.port", "25");
        //props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.class", "");
        props.put("mail.smtp.socketFactory.fallback", "false");

        Session mailSession = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("DataImport@211sandiego.org", "211d4t4!");
            }
        });

        // @debug.
        mailSession.setDebug(true);

        Message mail = new MimeMessage(mailSession);

        // Set the FROM, TO, DATE and SUBJECT fields.
        mail.setFrom(new InternetAddress("DataImport@211sandiego.org"));
        mail.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mgrEmailAddr));
        mail.setSentDate(new Date());
        mail.setSubject("Home Finder - " + clientName);

        // Email body.
        //mail.setText(msg.toString()); // Plain text message.
        mail.setContent(msg.toString(), "text/html; charset=utf-8");

        // Send email message.
        log.info("Send notification email to: " + mgrEmailAddr);
        Transport.send(mail);
    }
}
