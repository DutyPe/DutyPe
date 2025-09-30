# Azure Cosmos DB MongoDB API Setup Guide

This guide will help you set up Azure Cosmos DB with MongoDB API for the PartTimes backend application.

## Prerequisites

1. Azure subscription
2. Azure CLI installed (optional but recommended)

## Step 1: Create Azure Cosmos DB Account

### Using Azure Portal:
1. Go to [Azure Portal](https://portal.azure.com)
2. Click "Create a resource"
3. Search for "Azure Cosmos DB"
4. Click "Create"
5. Fill in the details:
   - **Subscription**: Your Azure subscription
   - **Resource Group**: Create new or use existing
   - **Account Name**: `parttimes-cosmos-db` (must be globally unique)
   - **API**: Azure Cosmos DB for MongoDB
   - **Location**: Choose closest to your users
   - **Capacity mode**: Serverless (for development) or Provisioned throughput (for production)
6. Click "Review + create" then "Create"

### Using Azure CLI:
```bash
# Create resource group
az group create --name parttimes-rg --location eastus

# Create Cosmos DB account
az cosmosdb create \
  --name parttimes-cosmos-db \
  --resource-group parttimes-rg \
  --locations regionName=eastus \
  --default-consistency-level Session \
  --enable-serverless
```

## Step 2: Get Connection Details

1. Go to your Cosmos DB account in Azure Portal
2. Navigate to "Connection String" in the left menu
3. Copy the **Primary Connection String** which looks like:
   ```
   mongodb://<username>:<password>@<host>:10255/<database>?ssl=true&replicaSet=globaldb&retrywrites=false&maxIdleTimeMS=120000&appName=@<username>@
   ```

## Step 3: Create Database and Collections

### Using Azure Portal:
1. Go to "Data Explorer" in your Cosmos DB account
2. Click "New Database"
3. Create database: `parttimes-backend`
4. Click "New Collection" and create the following collections:

#### Collection 1: users
- **Database**: `parttimes-backend`
- **Collection id**: `users`
- **Shard key**: `_id` (default)
- **Throughput**: 400 RU/s (or Auto-scale)

#### Collection 2: job_postings
- **Database**: `parttimes-backend`
- **Collection id**: `job_postings`
- **Shard key**: `_id` (default)
- **Throughput**: 400 RU/s (or Auto-scale)

#### Collection 3: job_applications
- **Database**: `parttimes-backend`
- **Collection id**: `job_applications`
- **Shard key**: `_id` (default)
- **Throughput**: 400 RU/s (or Auto-scale)

#### Collection 4: saved_jobs
- **Database**: `parttimes-backend`
- **Collection id**: `saved_jobs`
- **Shard key**: `_id` (default)
- **Throughput**: 400 RU/s (or Auto-scale)

#### Collection 5: notifications
- **Database**: `parttimes-backend`
- **Collection id**: `notifications`
- **Shard key**: `_id` (default)
- **Throughput**: 400 RU/s (or Auto-scale)

### Using Azure CLI:
```bash
# Create database
az cosmosdb sql database create \
  --account-name parttimes-cosmos-db \
  --resource-group parttimes-rg \
  --name parttimes-backend

# Create containers
az cosmosdb sql container create \
  --account-name parttimes-cosmos-db \
  --resource-group parttimes-rg \
  --database-name parttimes-backend \
  --name users \
  --partition-key-path "/email" \
  --throughput 400

az cosmosdb sql container create \
  --account-name parttimes-cosmos-db \
  --resource-group parttimes-rg \
  --database-name parttimes-backend \
  --name job_postings \
  --partition-key-path "/employerId" \
  --throughput 400

az cosmosdb sql container create \
  --account-name parttimes-cosmos-db \
  --resource-group parttimes-rg \
  --database-name parttimes-backend \
  --name job_applications \
  --partition-key-path "/workerId" \
  --throughput 400

az cosmosdb sql container create \
  --account-name parttimes-cosmos-db \
  --resource-group parttimes-rg \
  --database-name parttimes-backend \
  --name saved_jobs \
  --partition-key-path "/workerId" \
  --throughput 400

az cosmosdb sql container create \
  --account-name parttimes-cosmos-db \
  --resource-group parttimes-rg \
  --database-name parttimes-backend \
  --name notifications \
  --partition-key-path "/userId" \
  --throughput 400
```

## Step 4: Configure Environment Variables

### For Development:
Update `src/main/resources/application-dev.properties`:
```properties
spring.data.mongodb.uri=mongodb://vamsi298:Parttimes%40123@vamsi298.mongo.cosmos.azure.com:10255/parttimes-backend?ssl=true&replicaSet=globaldb&retrywrites=false&maxIdleTimeMS=120000&appName=@vamsi298@
spring.data.mongodb.database=parttimes-backend
```

### For Production:
Set the following environment variables:
```bash
export MONGODB_URI="mongodb://username:password@host:10255/database?ssl=true&replicaSet=globaldb&retrywrites=false&maxIdleTimeMS=120000&appName=@username@"
export MONGODB_DATABASE="parttimes-backend"
```

## Step 5: Test the Connection

1. Start your Spring Boot application
2. Check the logs for successful Cosmos DB connection
3. Test the API endpoints to ensure data operations work correctly

## Important Notes

1. **Partition Keys**: The partition keys are crucial for performance. Make sure to:
   - Use the same partition key values when querying
   - Distribute data evenly across partitions
   - Avoid cross-partition queries when possible

2. **Throughput**: 
   - Start with 400 RU/s per container for development
   - Monitor usage and scale up as needed
   - Consider using auto-scale for production

3. **Security**:
   - Never commit connection strings to version control
   - Use Azure Key Vault for production secrets
   - Enable firewall rules to restrict access

4. **Monitoring**:
   - Set up alerts for high RU consumption
   - Monitor query performance
   - Use Azure Monitor for comprehensive logging

## Troubleshooting

### Common Issues:

1. **Connection Timeout**: Check firewall settings and network connectivity
2. **Authentication Failed**: Verify URI and key are correct
3. **Container Not Found**: Ensure containers are created with correct names
4. **Partition Key Mismatch**: Verify partition key paths match your entity annotations

### Useful Commands:

```bash
# Check Cosmos DB status
az cosmosdb show --name parttimes-cosmos-db --resource-group parttimes-rg

# List containers
az cosmosdb sql container list --account-name parttimes-cosmos-db --resource-group parttimes-rg --database-name parttimes-backend

# Get connection strings
az cosmosdb keys list --name parttimes-cosmos-db --resource-group parttimes-rg
```

## Cost Optimization

1. **Use Serverless**: For development and low-traffic applications
2. **Optimize Queries**: Avoid cross-partition queries
3. **Index Management**: Create appropriate indexes for your query patterns
4. **Data Lifecycle**: Implement TTL for temporary data
5. **Monitoring**: Set up cost alerts and budgets
