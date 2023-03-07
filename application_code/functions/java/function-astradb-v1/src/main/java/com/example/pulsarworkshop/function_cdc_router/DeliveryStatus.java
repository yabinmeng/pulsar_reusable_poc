import lombok.Data;
import lombok.NonNull;

@Data
@NonNull
public class DeliveryStatus {
    private Boolean Late_delivery_risk;
    private String OrderStatus;
    private int OrderId;
    private int CategoryId;
    private String Delivery_Status;
    private String CustomerLname;
    private String CustomerCity;
    private String CustomerCountry;
    private String CustomerFname;
    private int CustomerId;
}
