Steps:
1. Create database "cdcdemo4" with keyspace "demo" in us-central-1 and download token details
2. Create streaming tenant "cdctest04" in uscentral1
3. Create table "demo.delivery_details" from script: CREATE TABLE demo.delivery_details ("CustomerId" int, "CategoryId" int, "CustomerCity" text, "CustomerCountry" text, "CustomerFname" text, "CustomerLname" text, "Delivery_Status" text, "Late_delivery_risk" boolean, "OrderId" int PRIMARY KEY, "OrderStatus" text ) WITH additional_write_policy = '99PERCENTILE' AND bloom_filter_fp_chance = 0.01 AND caching = {'keys': 'ALL', 'rows_per_partition': 'NONE'} AND comment = '' AND compaction = {'class': 'org.apache.cassandra.db.compaction.UnifiedCompactionStrategy'} AND compression = {'chunk_length_in_kb': '64', 'class': 'org.apache.cassandra.io.compress.LZ4Compressor'} AND crc_check_chance = 1.0 AND default_time_to_live = 0 AND gc_grace_seconds = 864000 AND max_index_interval = 2048 AND memtable_flush_period_in_ms = 0 AND min_index_interval = 128 AND read_repair = 'BLOCKING' AND speculative_retry = '99PERCENTILE';
4. Create table "demo.changes_by_order_id" from script: CREATE TABLE demo.changes_by_order_id (old_customer_id INT, old_late_delivery_risk BOOLEAN, old_order_status TEXT, old_delivery_status TEXT, old_customer_lname TEXT, old_customer_city TEXT, old_customer_country TEXT, old_customer_fname TEXT, old_order_id INT, old_category_id INT, old_updated_time BIGINT, new_customer_id INT, new_late_delivery_risk BOOLEAN, new_order_status TEXT, new_delivery_status TEXT, new_customer_lname TEXT, new_customer_city TEXT, new_customer_country TEXT, new_customer_fname TEXT, new_order_id INT, new_category_id INT, new_updated_time BIGINT, primary key ( ( new_order_id ) )  );
5. Enable CDC for tables "delivery_details" and "changes_by_order_id"
6. Generate a Database Admin token. Then, update secret, clientId, and tenant in config.properties for function.
Then, update secure bundle.
7. Run `mvn clean package`.
8. Go to streaming page and download client.conf
9. Copy client.conf into local Pulsar directory (after downloading Pulsar binaries)
10. Open console to Pulsar directory
11. In UI, create:
 namespaces;
    processing
    delivery-status
 persistent topics:
In $TENANT/processing:
  delivery-changes
  routed-changes
In $TENANT/delivery-status:
  pending-payment
  out-for-delivery
  complete
12. Update tenant and input topic in this command and run it:
  export CDC1=persistent://cdctest04/astracdc/data-367fdb1f-2fce-4284-ab38-6e8f1e986dbc-demo.delivery_details
  bin/pulsar-admin functions create --name changewriter --jar /Users/devin.bost/proj/demos/dehlivery_demo_astra/java/astra-demo/target/astra-demo-0.0.1-jar-with-dependencies.jar --classname com.datastax.demo.dehlivery.function.AstraChangeWriter -o persistent://cdctest04/processing/delivery-changes --tenant cdctest04 --namespace processing -i $CDC1
13. Update tenant and input topic in this command and run it:
  export CDC2=persistent://cdctest04/astracdc/data-367fdb1f-2fce-4284-ab38-6e8f1e986dbc-demo.changes_by_order_id
  bin/pulsar-admin functions create --name changerouter --jar /Users/devin.bost/proj/demos/dehlivery_demo_astra/java/astra-demo/target/astra-demo-0.0.1-jar-with-dependencies.jar --classname com.datastax.demo.dehlivery.function.AstraRouter -i $CDC2 -o persistent://$TENANT/processing/routed-changes --tenant $TENANT --namespace processing
14. Insert into delivery_details table to propagate schema:
  INSERT INTO demo.delivery_details ("CustomerId", "CategoryId", "CustomerCity", "CustomerCountry", "CustomerFname", "CustomerLname", "Delivery_Status", "Late_delivery_risk", "OrderId", "OrderStatus") VALUES ( 2432 , 95 , 'Delhi' , 'India' , 'Bob' , 'Smith' , 'Late delivery' , True , 11111 , 'PENDING_PAYMENT');
15. Create Organization Admin role token: https://astra.datastax.com/org/4c4a3356-5105-4354-b8ae-21afc799859e/settings/tokens
16. Create C* sink connector. # (If schema doesn't automatically appear, then troubleshoot.)
17. Create records:
INSERT INTO demo.delivery_details ("CustomerId", "CategoryId", "CustomerCity", "CustomerCountry", "CustomerFname", "CustomerLname", "Delivery_Status", "Late_delivery_risk", "OrderId", "OrderStatus") VALUES ( 2432 , 95 , 'Caguas' , 'Puerto Rico' , 'Jimmy' , 'Dean' , 'Late delivery' , True , 25454 , 'PENDING_PAYMENT');
INSERT INTO demo.delivery_details ("CustomerId", "CategoryId", "CustomerCity", "CustomerCountry", "CustomerFname", "CustomerLname", "Delivery_Status", "Late_delivery_risk", "OrderId", "OrderStatus") VALUES ( 2332 , 95 , 'Caguas' , 'Puerto Rico' , 'Jimmy' , 'Dean' , 'Late delivery' , True , 15454 , 'OUT_FOR_DELIVERY');
INSERT INTO demo.delivery_details ("CustomerId", "CategoryId", "CustomerCity", "CustomerCountry", "CustomerFname", "CustomerLname", "Delivery_Status", "Late_delivery_risk", "OrderId", "OrderStatus", "test") VALUES ( 2432 , 95 , 'Caguas' , 'Puerto Rico' , 'Jimmy' , 'Dean' , 'Late delivery' , True , 25454 , 'OUT_FOR_DELIVERY', 'example');
18. Subscribe pulsar-client:
bin/pulsar-client consume persistent://cdctest04/delivery-status/pending-payment -s test-dbost -n 0
bin/pulsar-client consume persistent://cdctest04/delivery-status/out-for-delivery -s test-dbost -n 0
bin/pulsar-client consume persistent://cdctest04/delivery-status/complete -s test-dbost -n 0

19.
INSERT INTO demo.delivery_details ("CustomerId", "CategoryId", "CustomerCity", "CustomerCountry", "CustomerFname", "CustomerLname", "Delivery_Status", "Late_delivery_risk", "OrderId", "OrderStatus") VALUES ( 1233 , 95 , 'Caguas' , 'Puerto Rico' , 'Jimmy' , 'Dean' , 'Late delivery' , True , 25454 , 'COMPLETE');

20. Observe that message flows through to clients