import app from "./app.js";
import db from "./config/database.js";
import env from "./config/env.js";
import { startPopularSearchJob } from "./jobs/popular-search.job.js";

startPopularSearchJob();

const startServer = async () => {
  try {
    await db.query("SELECT 1");
    console.log("Database connected");

    app.listen(env.port, () => {
      console.log(`Server running on port ${env.port}`);
    });
  } catch (error) {
    console.error("Server startup failed:", error);
    process.exit(1);
  }
};

startServer();
