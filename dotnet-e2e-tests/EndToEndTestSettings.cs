public sealed record EndToEndTestSettings(
    Uri AuditUrl,
    Uri WarehouseUrl,
    Uri InvoiceUrl,
    string KafkaBootstrapServers,
    string OrdersTopic,
    string BillingTopic)
{
    public static EndToEndTestSettings FromEnvironment()
    {
        return new EndToEndTestSettings(
            ReadUri("E2E_AUDIT_URL", "http://localhost:5081"),
            ReadUri("E2E_WAREHOUSE_URL", "http://localhost:5082"),
            ReadUri("E2E_INVOICE_URL", "http://localhost:5083"),
            Environment.GetEnvironmentVariable("E2E_KAFKA_BOOTSTRAP_SERVERS") ?? "localhost:9092",
            Environment.GetEnvironmentVariable("E2E_ORDERS_TOPIC") ?? "orders",
            Environment.GetEnvironmentVariable("E2E_BILLING_TOPIC") ?? "billing");
    }

    private static Uri ReadUri(string name, string fallback)
    {
        return new Uri(Environment.GetEnvironmentVariable(name) ?? fallback);
    }
}
