import { Router } from "express";
import { getEventsController } from "../controllers/events.controller.js";
import { optionalAuthMiddleware } from "../middlewares/auth.middleware.js";

const router = Router();

// 이벤트 상세 조회
router.get("/", getEventsController);
// 이벤트 상세 조회
router.get("/:id", optionalAuthMiddleware, getEventDetailController);

export default router;
