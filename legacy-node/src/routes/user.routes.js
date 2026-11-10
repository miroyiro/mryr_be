import { Router } from "express";
import { authMiddleware } from "../middlewares/auth.middleware.js";
import { getMyStreakController } from "../controllers/user.controller.js";

const router = Router();

// 내 스트릭 조회
router.get("/me/streak", authMiddleware, getMyStreakController);

export default router;
