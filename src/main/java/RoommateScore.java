package src.main.java;

public class RoommateScore implements Comparable<RoommateScore> {
    public String firstName;
    public String lastName;
    public int totalScore = 0;
    public int socialScore = 0;
    public int substanceScore = 0;

    public RoommateScore(String firstName, String lastName) {
        super();
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public int compareTo(RoommateScore compareObj) {
        // Descending order.
        return compareObj.totalScore - this.totalScore;
    }
}
