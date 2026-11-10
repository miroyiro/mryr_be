import express from "express";
import healthRoutes from "./health.routes.js";
import authRoutes from "./auth.routes.js";
import homeRoutes from "./home.routes.js";
import newsRoutes from "./news.routes.js";
import searchRoutes from "./search.routes.js";
import userRoutes from "./user.routes.js";
import calendarRoutes from "./calendar.routes.js";
import eventsRoutes from "./event.routes.js";
import animeRoutes from "./anime.routes.js";
import artistRoutes from "./artist.routes.js";
import adminRoutes from "./admin.routes.js";

const router = express.Router();

router.use("/health", healthRoutes);
router.use("/auth", authRoutes);
router.use("/home", homeRoutes);
router.use("/news", newsRoutes);
router.use("/search", searchRoutes);
router.use("/users", userRoutes);
router.use("/calendar", calendarRoutes);
router.use("/events", eventsRoutes);
router.use("/anime", animeRoutes);
router.use("/artists", artistRoutes);
router.use("/admin", adminRoutes);
export default router;
