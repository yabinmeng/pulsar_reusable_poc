package com.example.pulsarworkshop.function_cdc_router;
import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.pulsar.client.api.AuthenticationFactory;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.schema.GenericObject;
import org.apache.pulsar.client.api.schema.GenericRecord;
import org.apache.pulsar.common.schema.KeyValue;
import org.apache.pulsar.functions.api.Context;
import org.apache.pulsar.functions.api.Function;
import org.slf4j.Logger;

public class FunctionCdcRouter implements Function<GenericObject, DeliveryChange> {
    private String pulsarNamespace;
    private Logger logger;
    @Override
    public void initialize(Context context) throws Exception {
        this.logger = context.getLogger();
        this.setConfigs(context);
        Function.super.initialize(context);
    }
    @Override
    public DeliveryChange process(GenericObject input, Context context) throws Exception {
        KeyValue<GenericRecord, GenericRecord> keyValue = (KeyValue<GenericRecord, GenericRecord>) input.getNativeObject();
        GenericRecord keyGenObject = keyValue.getKey();
        GenericRecord valGenObject = keyValue.getValue();
        var orderStatus = (String)valGenObject.getField("new_order_status");
        var orderStatusClean = orderStatus.toLowerCase().replace('_', '-'); // make compatible with Pulsar convention
        // prepare downstream topic name
        var dynamicTopic = this.pulsarNamespace + "/" + orderStatusClean;
        DeliveryChange change = getDeliveryChange(keyGenObject, valGenObject);
// concurrency, ordering, schema changes, deletes, type of write (add a column for that). Latency.
        // provide code. end-to-end latency. compared to dynamo db latency. (Jauneet has that.)
        try {
            context.newOutputMessage(dynamicTopic, Schema.AVRO(DeliveryChange.class)).value(change).sendAsync();
        } catch (PulsarClientException e) {
            context.getLogger().error(e.toString());
        }
        return change;
    }

    private static DeliveryChange getDeliveryChange(GenericRecord keyGenObject, GenericRecord valGenObject) {
        var oldCustomerId = (Integer) valGenObject.getField("old_customer_id");
        var oldCategoryId = (Integer) valGenObject.getField("old_category_id");
        var oldCustomerCountry = (String) valGenObject.getField("old_customer_country");
        var oldCustomerCity = (String) valGenObject.getField("old_customer_city");
        var oldCustomerFname = (String) valGenObject.getField("old_customer_fname");
        var oldCustomerLname = (String) valGenObject.getField("old_customer_lname");
        var oldDeliveryStatus = (String) valGenObject.getField("old_delivery_status");
        var oldLateDeliveryRisk = (Boolean) valGenObject.getField("old_late_delivery_risk");
        var oldOrderId = (Integer) valGenObject.getField("old_order_id");
        var oldOrderStatus = (String) valGenObject.getField("old_order_status");
        var oldUpdatedTime = (Long) valGenObject.getField("old_updated_time");

        var newCustomerId = (Integer) valGenObject.getField("new_customer_id");
        var newCategoryId = (Integer) valGenObject.getField("new_category_id");
        var newCustomerCountry = (String) valGenObject.getField("new_customer_country");
        var newCustomerCity = (String) valGenObject.getField("new_customer_city");
        var newCustomerFname = (String) valGenObject.getField("new_customer_fname");
        var newCustomerLname = (String) valGenObject.getField("new_customer_lname");
        var newDeliveryStatus = (String) valGenObject.getField("new_delivery_status");
        var newLateDeliveryRisk = (Boolean) valGenObject.getField("new_late_delivery_risk");
        var newOrderId = (Integer) keyGenObject.getField("new_order_id");
        var newOrderStatus = (String) valGenObject.getField("new_order_status");
        var newUpdatedTime = (Long) valGenObject.getField("new_updated_time");

        DeliveryChange change = DeliveryChange.builder()
                .old_customer_id(oldCustomerId)
                .old_category_id(oldCategoryId)
                .old_customer_country(oldCustomerCountry)
                .old_customer_city(oldCustomerCity)
                .old_customer_fname(oldCustomerFname)
                .old_customer_lname(oldCustomerLname)
                .old_delivery_status(oldDeliveryStatus)
                .old_late_delivery_risk(oldLateDeliveryRisk)
                .old_order_id(oldOrderId)
                .old_order_status(oldOrderStatus)
                .old_updated_time(oldUpdatedTime)
                .new_customer_id(newCustomerId)
                .new_category_id(newCategoryId)
                .new_customer_country(newCustomerCountry)
                .new_customer_city(newCustomerCity)
                .new_customer_fname(newCustomerFname)
                .new_customer_lname(newCustomerLname)
                .new_delivery_status(newDeliveryStatus)
                .new_late_delivery_risk(newLateDeliveryRisk)
                .new_order_id(newOrderId)
                .new_order_status(newOrderStatus)
                .new_updated_time(newUpdatedTime).build();
        return change;
    }

    @Override
    public void close() throws Exception {
        Function.super.close();
    }
    public void setConfigs(Context context){
        // Next step would be to use a function config instead of this properties file.
        try (InputStream input = AstraChangeWriter.class.getResourceAsStream("/config.properties")) {
            Properties prop = new Properties();
            prop.load(input);
            this.pulsarNamespace = prop.getProperty("PULSAR_NAMESPACE");

        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(0);
        }
    }
}
