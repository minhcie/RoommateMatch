package src.main.java;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class DbRoommate {
    private static final Logger log = Logger.getLogger(DbRoommate.class.getName());

    public long id = 0;
    public long clientId;
    public boolean rentalSubsidy = false;
    public String subsidyType;
    public String pshSource;
    public String rrhSource;
    public String incomeSource;
    public String monthlyIncomeAmount;
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

    public String caseNumber;
    public String firstName;
    public String lastName;

    public void insert(Connection conn) {
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("INSERT INTO roommate (clientId, rentalSubsidy, subsidyType, ");
            sb.append("pshSource, rrhSource, incomeSource, monthlyIncomeAmount, ");
            sb.append("cleanupAfterThemselves, quietBy10pm, likeToTalk, keepToThemselves, ");
            sb.append("noPet, noOvernightGuest, noSmoke, noAlcohol, noMarijuana, ");
            sb.append("noIllegalSubstance) ");
            sb.append("VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

            PreparedStatement ps = conn.prepareStatement(sb.toString(),
                                                         Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, this.clientId);
            ps.setBoolean(2, this.rentalSubsidy);
            ps.setString(3, SqlString.encode(this.subsidyType));
            ps.setString(4, SqlString.encode(this.pshSource));
            ps.setString(5, SqlString.encode(this.rrhSource));
            ps.setString(6, SqlString.encode(this.incomeSource));
            ps.setString(7, SqlString.encode(this.monthlyIncomeAmount));
            ps.setBoolean(8, this.cleanupAfterThemselves);
            ps.setBoolean(9, this.quietBy10pm);
            ps.setBoolean(10, this.likeToTalk);
            ps.setBoolean(11, this.keepToThemselves);
            ps.setBoolean(12, this.noPet);
            ps.setBoolean(13, this.noOvernightGuest);
            ps.setBoolean(14, this.noSmoke);
            ps.setBoolean(15, this.noAlcohol);
            ps.setBoolean(16, this.noMarijuana);
            ps.setBoolean(17, this.noIllegalSubstance);

            int out = ps.executeUpdate();
            if (out == 0) {
                log.info("Failed to insert roommate record!");
            }
            else {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    this.id = rs.getLong(1);
                }
            }
        }
        catch (SQLException sqle) {
            log.error("SQLException DbRoommate.insert(): " + sqle);
        }
        catch (Exception e) {
            log.error("Exception DbRoommate.insert(): " + e);
        }
    }

    public void update(Connection conn) {
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("UPDATE roommate SET clientId = ?, rentalSubsidy = ?, subsidyType = ?, ");
            sb.append("pshSource = ?, rrhSource = ?, incomeSource = ?, monthlyIncomeAmount = ?, ");
            sb.append("cleanupAfterThemselves = ?, quietBy10pm = ?, likeToTalk = ?, ");
            sb.append("keepToThemselves = ?, noPet = ?, noOvernightGuest = ?, noSmoke = ?, ");
            sb.append("noAlcohol = ?, noMarijuana = ?, noIllegalSubstance = ? ");
            sb.append("WHERE id = " + this.id);

            PreparedStatement ps = conn.prepareStatement(sb.toString());
            ps.setLong(1, this.clientId);
            ps.setBoolean(2, this.rentalSubsidy);
            ps.setString(3, SqlString.encode(this.subsidyType));
            ps.setString(4, SqlString.encode(this.pshSource));
            ps.setString(5, SqlString.encode(this.rrhSource));
            ps.setString(6, SqlString.encode(this.incomeSource));
            ps.setString(7, SqlString.encode(this.monthlyIncomeAmount));
            ps.setBoolean(8, this.cleanupAfterThemselves);
            ps.setBoolean(9, this.quietBy10pm);
            ps.setBoolean(10, this.likeToTalk);
            ps.setBoolean(11, this.keepToThemselves);
            ps.setBoolean(12, this.noPet);
            ps.setBoolean(13, this.noOvernightGuest);
            ps.setBoolean(14, this.noSmoke);
            ps.setBoolean(15, this.noAlcohol);
            ps.setBoolean(16, this.noMarijuana);
            ps.setBoolean(17, this.noIllegalSubstance);

            int out = ps.executeUpdate();
            if (out == 0) {
                log.info("Failed to update roommate record!");
            }
        }
        catch (SQLException sqle) {
            log.error("SQLException DbRoommate.update(): " + sqle);
        }
        catch (Exception e) {
            log.error("Exception DbRoommate.update(): " + e);
        }
    }

    public static DbRoommate findByClientId(Connection conn, long clientId) {
        DbRoommate rec = null;
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("SELECT id, clientId, rentalSubsidy, subsidyType, pshSource, ");
            sb.append("rrhSource, incomeSource, monthlyIncomeAmount, cleanupAfterThemselves, ");
            sb.append("quietBy10pm, likeToTalk, keepToThemselves, noPet, noOvernightGuest, ");
            sb.append("noSmoke, noAlcohol, noMarijuana, noIllegalSubstance ");
            sb.append("FROM roommate ");
            sb.append("WHERE clientId = " + clientId);

            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(sb.toString());
            if (rs.next()) {
                rec = new DbRoommate();
                rec.id = rs.getLong("id");
                rec.clientId = rs.getLong("clientId");
                rec.rentalSubsidy = rs.getBoolean("rentalSubsidy");
                rec.subsidyType = rs.getString("subsidyType");
                rec.pshSource = rs.getString("pshSource");
                rec.rrhSource = rs.getString("rrhSource");
                rec.incomeSource = rs.getString("incomeSource");
                rec.monthlyIncomeAmount = rs.getString("monthlyIncomeAmount");
                rec.cleanupAfterThemselves = rs.getBoolean("cleanupAfterThemselves");
                rec.quietBy10pm = rs.getBoolean("quietBy10pm");
                rec.likeToTalk = rs.getBoolean("likeToTalk");
                rec.keepToThemselves = rs.getBoolean("keepToThemselves");
                rec.noPet = rs.getBoolean("noPet");
                rec.noOvernightGuest = rs.getBoolean("noOvernightGuest");
                rec.noSmoke = rs.getBoolean("noSmoke");
                rec.noAlcohol = rs.getBoolean("noAlcohol");
                rec.noMarijuana = rs.getBoolean("noMarijuana");
                rec.noIllegalSubstance = rs.getBoolean("noIllegalSubstance");
            }

            rs.close();
            statement.close();
        }
        catch (SQLException sqle) {
             log.error("SQLException in DbRoommate.findByClientId(): " + sqle);
        }
        catch (Exception e) {
             log.error("Exception in DbRoommate.findByClientId(): " + e);
        }
        return rec;
    }

    public static List<DbRoommate> findAll(Connection conn) {
        List<DbRoommate> results = new ArrayList<DbRoommate>();
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("SELECT r.id, r.clientId, r.rentalSubsidy, r.subsidyType, ");
            sb.append("r.pshSource, r.rrhSource, r.incomeSource, r.monthlyIncomeAmount, ");
            sb.append("r.cleanupAfterThemselves, r.quietBy10pm, r.likeToTalk, ");
            sb.append("r.keepToThemselves, r.noPet, r.noOvernightGuest, r.noSmoke, ");
            sb.append("r.noAlcohol, r.noMarijuana, r.noIllegalSubstance, c.caseNumber, ");
            sb.append("c.firstName, c.lastName ");
            sb.append("FROM roommate r ");
            sb.append("INNER JOIN client c ON r.clientId = c.id");

            Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(sb.toString());
            while (rs.next()) {
                DbRoommate rec = new DbRoommate();
                rec.id = rs.getLong("id");
                rec.clientId = rs.getLong("clientId");
                rec.rentalSubsidy = rs.getBoolean("rentalSubsidy");
                rec.subsidyType = rs.getString("subsidyType");
                rec.pshSource = rs.getString("pshSource");
                rec.rrhSource = rs.getString("rrhSource");
                rec.incomeSource = rs.getString("incomeSource");
                rec.monthlyIncomeAmount = rs.getString("monthlyIncomeAmount");
                rec.cleanupAfterThemselves = rs.getBoolean("cleanupAfterThemselves");
                rec.quietBy10pm = rs.getBoolean("quietBy10pm");
                rec.likeToTalk = rs.getBoolean("likeToTalk");
                rec.keepToThemselves = rs.getBoolean("keepToThemselves");
                rec.noPet = rs.getBoolean("noPet");
                rec.noOvernightGuest = rs.getBoolean("noOvernightGuest");
                rec.noSmoke = rs.getBoolean("noSmoke");
                rec.noAlcohol = rs.getBoolean("noAlcohol");
                rec.noMarijuana = rs.getBoolean("noMarijuana");
                rec.noIllegalSubstance = rs.getBoolean("noIllegalSubstance");

                rec.caseNumber = rs.getString("caseNumber");
                rec.firstName = rs.getString("firstName");
                rec.lastName = rs.getString("lastName");
                results.add(rec);
            }

            rs.close();
            statement.close();
        }
        catch (SQLException sqle) {
             log.error("SQLException in DbRoommate.findAll(): " + sqle);
        }
        catch (Exception e) {
             log.error("Exception in DbRoommate.findAll(): " + e);
        }
        return results;
    }
}
