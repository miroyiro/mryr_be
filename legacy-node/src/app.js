import express from "express";
import cookieParser from "cookie-parser";

import routes from "./routes/index.js";
import { errorMiddleware } from "./middlewares/error.middleware.js";

const app = express();

app.use(express.json());
app.use(cookieParser());

app.use("/api", routes);

app.use(errorMiddleware);

export default app;
