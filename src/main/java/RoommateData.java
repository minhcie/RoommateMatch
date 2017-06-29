package src.main.java;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RoommateData implements Serializable {
    static final long serialVersionUID = 1L;

    // Data source.
    String source;

    // Client detail info.
    public String clientUid; // Used as client caseNumber.
    public long programId = 0;
    public Date entryDate;
    public Date exitDate;
    public String firstName;
    public String lastName;
    public String gender;
    public String ssn;
    public Date dob;
    public String race;
    public String ethnicity;
    public boolean veteran = false;
    public boolean disabled = false;
    public boolean rentalSubsidy = false;
    public String subsidyType;
    public String pshSource;
    public String rrhSource;
    public String incomeSource;
    public String monthlyIncomeAmount;

    // Matching criteria: What do you want in a roommate?
    public boolean cleanupAfterThemselves = false;
    public boolean quietBy10pm = false;
    public boolean likeToTalk = false;
    public boolean keepToThemselves = false;
    public boolean noPet = false;
    public boolean noOvernightGuest = false;
    public boolean noSmoke = false;
    public boolean noAlcohol = false;
    public boolean noMarijuana = false;
    public boolean noIllegalSubstance = false;

    @Override
    public String toString() {
        if (this.dob != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
            return this.source + " - " +
                   "name: " + this.firstName + " " + this.lastName +
                   ", dob: " + sdf.format(this.dob) + ", ssn: " + this.ssn +
                   ", case number: " + this.clientUid;
        }
        else {
            return this.source + " - " +
                   "name: " + this.firstName + " " + this.lastName +
                   ", ssn: " + this.ssn + ", case number: " + this.clientUid;
        }
    }
}
