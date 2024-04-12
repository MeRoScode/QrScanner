package ir.meros.qrscanner;

public class Person {

    private String seatFullName;
    private String seatType;
    private String ticketId;

    public String getSeatFullName() {
        return seatFullName;
    }

    public void setSeatFullName(String seatFullName) {
        this.seatFullName = seatFullName;
    }

    public String getSeatType() {
        return seatType;
    }

    public void setSeatType(String seatType) {
        this.seatType = seatType;
    }

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }
}
