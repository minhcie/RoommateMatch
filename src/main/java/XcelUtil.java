package src.main.java;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.apache.log4j.Logger;

public class XcelUtil {

    private static final Logger log = Logger.getLogger(XcelUtil.class.getName());

    public static List<RoommateData> readData() throws IOException, Exception {
        // Roommnate matching data file.
        log.info("Reading excel file (Roommate_Matching_Report.xlsx)...");
        File xcel = new File("Roommate_Matching_Report.xlsx");
        if (!xcel.exists()) {
            log.error("File not found!");
            return null;
        }

        Map<String, RoommateData> roommateMap = new HashMap<String, RoommateData>();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        Row row;
        Cell cell;

        // Get the workbook object for xlsx file.
        XSSFWorkbook wBook = new XSSFWorkbook(new FileInputStream(xcel));

        // Read client data from the sheet 1.
        XSSFSheet sheet = wBook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.iterator();
        while (rowIterator.hasNext()) {
            row = rowIterator.next();

            // Ignore column header rows.
            if (row.getRowNum() == 0 || row.getRowNum() == 1) {
                continue;
            }

            // Iterate to all cells (including empty cell).
            RoommateData roommate = new RoommateData();
            int minColIndex = row.getFirstCellNum();
            int maxColIndex = row.getLastCellNum();
            for (int colIndex = minColIndex; colIndex < maxColIndex; colIndex++) {
                String cellValue = null;
                cell = row.getCell(colIndex);
                cellValue = getCellValue(cell);

                if (cellValue == null || cellValue.length() <= 0) {
                    // Ignore empty cell.
                    continue;
                }
                else if (colIndex > 20) {
                    // Done with this row.
                    break;
                }

                switch (colIndex) {
                    case 1:
                        roommate.clientUid = cellValue;
                        break;
                    case 2:
                        if (cellValue.length() > 0) {
                            roommate.entryDate = sdf.parse(cellValue);
                        }
                        break;
                    case 3:
                        if (cellValue.length() > 0) {
                            roommate.exitDate = sdf.parse(cellValue);
                        }
                        break;
                    case 5:
                        String clientName = cellValue;
                        String[] parts = clientName.split(" ");
                        roommate.firstName = parts[0];
                        roommate.lastName = parts[1];
                        break;
                    case 6:
                        roommate.ssn = cellValue;
                        break;
                    case 7:
                        roommate.gender = cellValue;
                        break;
                    case 8:
                        if (cellValue.length() > 0) {
                            roommate.dob = sdf.parse(cellValue);
                        }
                        break;
                    case 9:
                        roommate.race = cellValue;
                        break;
                    case 10:
                        roommate.ethnicity = cellValue;
                        break;
                    case 11:
                        if (cellValue.contains("Yes")) {
                            roommate.veteran = true;
                        }
                        break;
                    case 12:
                        if (cellValue.contains("Yes")) {
                            roommate.disabled = true;
                        }
                        break;
                    case 13:
                        if (cellValue.contains("Yes")) {
                            roommate.rentalSubsidy = true;
                        }
                        break;
                    case 14:
                        roommate.subsidyType = cellValue;
                        break;
                    case 15:
                        roommate.pshSource = cellValue;
                        break;
                    case 17:
                        roommate.rrhSource = cellValue;
                        break;
                    case 20:
                        roommate.monthlyIncomeAmount = cellValue;
                        break;
                    default:
                        break;
                }
            }

            // Save roommate data.
            if (roommate.clientUid != null && !roommateMap.containsKey(roommate.clientUid)) {
                log.debug("insert client " + roommate.clientUid + " into roommateMap");
                roommateMap.put(roommate.clientUid, roommate);
            }
        }

        // Read income source from the sheet 2.
        sheet = wBook.getSheetAt(1);
        rowIterator = sheet.iterator();
        while (rowIterator.hasNext()) {
            row = rowIterator.next();

            // Ignore column header rows.
            if (row.getRowNum() == 0 || row.getRowNum() == 1) {
                continue;
            }

            // Get client unique ID.
            cell = row.getCell(1);
            String clientUid = getCellValue(cell);
            if (clientUid == null || clientUid.length() <= 0) {
                continue;
            }

            // Find and update client data.
            RoommateData roommate = roommateMap.get(clientUid);
            if (roommate != null) {
                cell = row.getCell(4);
                roommate.incomeSource = getCellValue(cell);
            }
        }

        // Read matching criteria from the sheet 3.
        sheet = wBook.getSheetAt(2);
        rowIterator = sheet.iterator();
        while (rowIterator.hasNext()) {
            row = rowIterator.next();

            // Ignore column header rows.
            if (row.getRowNum() == 0 || row.getRowNum() == 1) {
                continue;
            }

            // Get client unique ID.
            cell = row.getCell(1);
            String clientUid = getCellValue(cell);
            if (clientUid == null || clientUid.length() <= 0) {
                continue;
            }

            // Find and update client data.
            RoommateData roommate = roommateMap.get(clientUid);
            if (roommate != null) {
                int maxColIndex = row.getLastCellNum();
                for (int colIndex = 2; colIndex < maxColIndex; colIndex++) {
                    String cellValue = null;
                    cell = row.getCell(colIndex);
                    cellValue = getCellValue(cell);

                    if (cellValue == null || cellValue.length() <= 0) {
                        // Ignore empty cell.
                        continue;
                    }
                    else if (colIndex > 11) {
                        // Done with this row.
                        break;
                    }

                    switch (colIndex) {
                        case 2:
                            if (cellValue.contains("Yes")) {
                                roommate.cleanupAfterThemselves = true;
                            }
                            break;
                        case 3:
                            if (cellValue.contains("Yes")) {
                                roommate.quietBy10pm = true;
                            }
                            break;
                        case 4:
                            if (cellValue.contains("Yes")) {
                                roommate.likeToTalk = true;
                            }
                            break;
                        case 5:
                            if (cellValue.contains("Yes")) {
                                roommate.keepToThemselves = true;
                            }
                            break;
                        case 6:
                            if (cellValue.contains("Yes")) {
                                roommate.noPet = true;
                            }
                            break;
                        case 7:
                            if (cellValue.contains("Yes")) {
                                roommate.noOvernightGuest = true;
                            }
                            break;
                        case 8:
                            if (cellValue.contains("Yes")) {
                                roommate.noSmoke = true;
                            }
                            break;
                        case 9:
                            if (cellValue.contains("Yes")) {
                                roommate.noAlcohol = true;
                            }
                            break;
                        case 10:
                            if (cellValue.contains("Yes")) {
                                roommate.noMarijuana = true;
                            }
                            break;
                        case 11:
                            if (cellValue.contains("Yes")) {
                                roommate.noIllegalSubstance = true;
                            }
                            break;
                        default:
                            break;
                    }
                }

                // @debug.
                //debug(roommate);
            }
        }

        //log.debug("roommateMap.size(): " + roommateMap.size());
        List<RoommateData> data = new ArrayList<RoommateData>(roommateMap.values());
        return data;
    }

    private static String getCellValue(Cell cell) {
        String retVal = "";
        if (cell == null) {
            return retVal;
        }

        switch (cell.getCellType()) {
            case Cell.CELL_TYPE_BOOLEAN:
                retVal = "" + cell.getBooleanCellValue();
                break;

            case Cell.CELL_TYPE_STRING:
                retVal = cell.getStringCellValue();
                break;

            case Cell.CELL_TYPE_NUMERIC:
                retVal = isNumberOrDate(cell);
                break;

            case Cell.CELL_TYPE_BLANK:
            default:
                retVal = "";
        }
        return retVal.trim();
    }

    private static String isNumberOrDate(Cell cell) {
        String retVal;
        if (HSSFDateUtil.isCellDateFormatted(cell)) {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
            retVal = sdf.format(cell.getDateCellValue());
        }
        else {
            DataFormatter formatter = new DataFormatter();
            retVal = formatter.formatCellValue(cell);
        }
        return retVal;
    }

    private static void debug(RoommateData data) {
        log.debug("------------------------------");
        log.debug("clientUid: " + data.clientUid);
        log.debug("entryDate: " + data.entryDate);
        log.debug("exitDate: " + data.exitDate);
        log.debug("firstName: " + data.firstName);
        log.debug("lastName: " + data.lastName);
        log.debug("gender: " + data.gender);
        log.debug("ssn: " + data.ssn);
        log.debug("dob: " + data.dob);
        log.debug("race: " + data.race);
        log.debug("veteran: " + data.veteran);
        log.debug("disabled: " + data.disabled);
        log.debug("rentalSubsidy: " + data.rentalSubsidy);
        log.debug("subsidyType: " + data.subsidyType);
        log.debug("pshSource: " + data.pshSource);
        log.debug("rrhSource: " + data.rrhSource);
        log.debug("incomeSource: " + data.incomeSource);
        log.debug("monthlyIncomeAmount: " + data.monthlyIncomeAmount);
        log.debug("cleanupAfterThemselves: " + data.cleanupAfterThemselves);
        log.debug("quietBy10pm: " + data.quietBy10pm);
        log.debug("likeToTalk: " + data.likeToTalk);
        log.debug("keepToThemselves: " + data.keepToThemselves);
        log.debug("noPet: " + data.noPet);
        log.debug("noOvernightGuest: " + data.noOvernightGuest);
        log.debug("noSmoke: " + data.noSmoke);
        log.debug("noAlcohol: " + data.noAlcohol);
        log.debug("noMarijuana: " + data.noMarijuana);
        log.debug("noIllegalSubstance: " + data.noIllegalSubstance);
        log.debug("------------------------------");
    }
}
