import { Router } from "express";
import { authMiddleware } from "../middlewares/auth.middleware.js";
import {
  likeNewsController,
  unlikeNewsController,
} from "../controllers/news.controller.js";

const router = Router();

// 소식 좋아요 등록
router.post("/:id/like", authMiddleware, likeNewsController);
// 소식 좋아요 취소
router.delete("/:id/like", authMiddleware, unlikeNewsController);

export default router;
