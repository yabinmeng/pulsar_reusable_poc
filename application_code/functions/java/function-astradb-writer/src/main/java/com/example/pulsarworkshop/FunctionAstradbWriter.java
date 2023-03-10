package com.example.pulsarworkshop;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.pulsar.client.api.schema.GenericObject;
import org.apache.pulsar.client.api.schema.GenericRecord;
import org.apache.pulsar.common.schema.KeyValue;
import org.apache.pulsar.functions.api.Context;
import org.apache.pulsar.functions.api.Function;
import org.slf4j.Logger;
import java.time.Instant;

public class FunctionAstradbWriter implements Function<GenericObject, DeliveryChange> {
    private Logger logger;
    private CqlSession astraDbSession;
    private PreparedStatement preparedSelect;
    private String dbClientId;
    private String dbClientSecret;
    public FunctionAstradbWriter() {
        
    }
    public void setConfigs(Context context){
        // For future, use Pulsar Secret instead of config.properties for improved security.
        try (InputStream input = FunctionAstradbWriter.class.getResourceAsStream("/config.properties")) {
            Properties prop = new Properties();
            prop.load(input);
            this.dbClientSecret = prop.getProperty("DB_CLIENT_SECRET");
            this.dbClientId = prop.getProperty("DB_CLIENT_ID");

        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(0);
        }
    }
    public void prepareQueries(){
        String selectQuery = "SELECT new_customer_id, new_order_status, new_delivery_status, new_customer_lname, new_customer_city, new_customer_country, new_customer_fname, new_order_id, new_category_id, new_updated_time, new_late_delivery_risk FROM changes_by_order_id WHERE new_order_id = ?";
        this.preparedSelect = this.astraDbSession.prepare(selectQuery);
    }
    @Override
    public void initialize(Context context){
        this.setConfigs(context);
        this.astraDbSession = CqlSession.builder()
                .withCloudSecureConnectBundle(FunctionAstradbWriter.class.getResourceAsStream("/secure-connect-astra-demo.zip"))
                .withAuthCredentials(this.dbClientId,this.dbClientSecret)
                .withKeyspace("ddemo")
                .build();

        this.prepareQueries();
        this.logger = context.getLogger();
    }

    @Override
    public DeliveryChange process(GenericObject input, Context context) throws Exception {
        KeyValue<GenericRecord, GenericRecord> keyValue = (KeyValue<GenericRecord, GenericRecord>) input.getNativeObject();
        var orderId = (Integer)keyValue.getKey().getField("OrderId");
        BoundStatement boundStatement = this.preparedSelect.bind(orderId);
        var output = this.astraDbSession.execute(boundStatement);
        var outputs = output.all();
        DeliveryChange change = new DeliveryChange();

        if(outputs.size() > 0) { // i.e. if changes exist in target table
            var row = outputs.get(0);
            // move "new" record in table to old record in object
            var lastUpdatedTime = row.getLong("new_updated_time");
            if (lastUpdatedTime <= Instant.now().toEpochMilli()){
                moveOldChanges(change, row, lastUpdatedTime);
                if (keyValue.getValue() == null){
                    setNullChanges(change, orderId);
                }
                else {
                    setNewChanges(change, keyValue);
                }
                return change;
            }
            else {
                return null;
            }
        }
        else {
            // Then, the record is for a new order, and we should write a new entry.
            setNewChanges(change, keyValue);
            return change;
        }
    }
    private static void moveOldChanges(DeliveryChange change, Row row, long lastUpdatedTime) {
        change.setOld_updated_time(lastUpdatedTime);
        // This would be cleaner if we used the C* mapper.
        change.setOld_customer_id(row.getInt("new_customer_id"));
        change.setOld_category_id(row.getInt("new_category_id"));
        change.setOld_customer_country(row.getString("new_customer_country"));
        change.setOld_customer_city(row.getString("new_customer_city"));
        change.setOld_customer_fname(row.getString("new_customer_fname"));
        change.setOld_customer_lname(row.getString("new_customer_lname"));
        change.setOld_delivery_status(row.getString("new_delivery_status"));
        change.setOld_order_status(row.getString("new_order_status"));
        change.setOld_order_id(row.getInt("new_order_id"));
        change.setOld_late_delivery_risk(row.getBoolean("new_late_delivery_risk"));
    }

    public void setNullChanges(DeliveryChange change, Integer orderId){
        change.setNew_customer_id(null);
        change.setNew_category_id(null);
        change.setNew_customer_country(null);
        change.setNew_customer_city(null);
        change.setNew_customer_fname(null);
        change.setNew_customer_lname(null);
        change.setNew_order_status(null);
        change.setNew_delivery_status(null);
        change.setNew_late_delivery_risk(null);
        change.setNew_order_id(orderId);
        change.setNew_updated_time(Instant.now().toEpochMilli());
    }
    public void setNewChanges(DeliveryChange change, KeyValue<GenericRecord, GenericRecord> keyValue){
        GenericRecord keyGenObject = keyValue.getKey();
        GenericRecord valGenObject = keyValue.getValue();
        var customerId = (Integer)valGenObject.getField("CustomerId");
        var categoryId = (Integer)valGenObject.getField("CategoryId");
        var customerCountry = (String)valGenObject.getField("CustomerCountry");
        var customerCity = (String)valGenObject.getField("CustomerCity");
        var customerFname = (String)valGenObject.getField("CustomerFname");
        var customerLname = (String)valGenObject.getField("CustomerLname");
        var customerOrderStatus = (String)valGenObject.getField("OrderStatus");
        var deliveryStatus = (String)valGenObject.getField("Delivery_Status");
        var lateDeliveryRisk = (Boolean)valGenObject.getField("Late_delivery_risk");
        var orderId = (Integer)keyGenObject.getField("OrderId");

        change.setNew_customer_id(customerId);
        change.setNew_category_id(categoryId);
        change.setNew_customer_country(customerCountry);
        change.setNew_customer_city(customerCity);
        change.setNew_customer_fname(customerFname);
        change.setNew_customer_lname(customerLname);
        change.setNew_order_status(customerOrderStatus);
        change.setNew_delivery_status(deliveryStatus);
        change.setNew_late_delivery_risk(lateDeliveryRisk);
        change.setNew_order_id(orderId);
        change.setNew_updated_time(Instant.now().toEpochMilli());
    }

    @Override
    public void close() throws Exception {
        if (this.astraDbSession != null)
            this.astraDbSession.close();
    }
}
