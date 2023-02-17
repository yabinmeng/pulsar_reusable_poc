# Update to the latest Helm dependencies
helm dependency update

# Install Pulsar
helm install pulsar -f <values_xxx>.yaml datastax-pulsar/pulsar