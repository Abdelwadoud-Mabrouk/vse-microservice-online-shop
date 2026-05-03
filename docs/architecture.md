# Architecture Overview

## Initial decisions
- shop-service and billing-service are implemented in Java / Spring Boot. (Amine Grioui)
- invoice-service, warehouse-service and audit-service are implemented in .NET. (Abdelwadoud Mabrouk)
- RabbitMQ is used for asynchronous communication.
- PostgreSQL is used as relational database.