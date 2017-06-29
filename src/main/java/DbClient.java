package src.main.java;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.apache.log4j.Logger;

public class DbClient {
    private static final Logger log = Logger.getLogger(DbClient.class.getName());

    public long id = 0;
    public long organizationId;
    public long genderId;
    public String firstName;
    public String lastName;
    public Date dob;
    public String ssn;
    public String caseNumber;
    public int ethnicity;
    public boolean active = true;
    public UUID etoEnterpriseId;
    public long etoParticipantSiteId;
    public long etoSubjectId;

    public void insert(Connection conn) {
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("INSERT INTO client (organizationId, genderId, firstName, ");
            sb.append("lastName, dob, ssn, caseNumber, ethnicity, active, ");
            sb.append("etoEnterpriseId, etoParticipantSiteId, etoSubjectId) ");
            sb.append("VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::uuid, ?, ?)");

            PreparedStatement ps = conn.prepareStatement(sb.toString(),
                                                         Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, this.organizationId);
            ps.setLong(2, this.genderId);
            ps.setString(3, SqlString.encode(this.firstName));
            ps.setString(4, SqlString.encode(this.lastName));
            java.sql.Date sqlDate = new java.sql.Date(this.dob.getTime());
            ps.setDate(5, sqlDate);
            ps.setString(6, SqlString.encode(this.ssn));
            ps.setString(7, SqlString.encode(this.caseNumber));
            ps.setInt(8, this.ethnicity);
            ps.setBoolean(9, this.active);
            ps.setObject(10, this.etoEnterpriseId);
            ps.setLong(11, this.etoParticipantSiteId);
            ps.setLong(12, this.etoSubjectId);

            int out = ps.executeUpdate();
            if (out == 0) {
                log.info("Failed to insert client record!");
            }
            else {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    this.id = rs.getLong(1);
                }
            }
        }
        catch (SQLException sqle) {
            log.error("SQLException DbClient.insert(): " + sqle);
        }
        catch (Exception e) {
            log.error("Exception DbClient.insert(): " + e);
        }
    }

    public static DbClient findByCaseNumber(Connection conn, long organizationId,
                                            String caseNumber) {
        DbClient client = null;
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("SELECT id, organizationId, genderId, firstName, lastName, dob, ");
            sb.append("ssn, caseNumber, etoEnterpriseId, etoParticipantSiteId, etoSubjectId ");
            sb.append("FROM client ");
            sb.append("WHERE organizationId = " + organizationId);
            sb.append("  AND caseNumber = '" + SqlString.encode(caseNumber) + "'");

            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(sb.toString());
            if (rs.next()) {
                client = new DbClient();
                client.id = rs.getLong("id");
                client.organizationId = rs.getLong("organizationId");
                client.genderId = rs.getLong("genderId");
                client.firstName = rs.getString("firstName");
                client.lastName = rs.getString("lastName");
                client.dob = rs.getDate("dob");
                client.ssn = rs.getString("ssn");
                client.caseNumber = rs.getString("caseNumber");
                client.etoEnterpriseId = (UUID)rs.getObject("etoEnterpriseId");
                client.etoParticipantSiteId = rs.getLong("etoParticipantSiteId");
                client.etoSubjectId = rs.getLong("etoSubjectId");
            }

            rs.close();
            statement.close();
        }
        catch (SQLException sqle) {
             log.error("SQLException in DbClient.findByCaseNumber(): " + sqle);
        }
        catch (Exception e) {
             log.error("Exception in DbClient.findByCaseNumber(): " + e);
        }
        return client;
    }
}
