// In this file you can configure migrate-mongo

const dbUrl = process.env.DB_HOST;
const dbPort = process.env.DB_PORT;
const dbName = process.env.MONGO_DB;

const config = {
    mongodb: {
        url: "mongodb://" + dbUrl + ":" + dbPort,

        databaseName: dbName,

        options: {
            useNewUrlParser: true,
            useUnifiedTopology: true // removes a deprecation warning when connecting
            //   connectTimeoutMS: 3600000, // increase connection timeout to 1 hour
            //   socketTimeoutMS: 3600000, // increase socket timeout to 1 hour
        }
    },

    // The migrations dir, can be an relative or absolute path. Only edit this when really necessary.
    migrationsDir: "users-migration/migrations",

    // The mongodb collection where the applied changes are stored. Only edit this when really necessary.
    changelogCollectionName: "changelog"
};

//Return the config as a promise
module.exports = config;
