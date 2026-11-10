import { Router } from "express";
import {
  searchController,
  popularSearchController,
  recentSearchController,
  deleteRecentSearchController,
} from "../controllers/search.controller.js";
import { authMiddleware } from "../middlewares/auth.middleware.js";

const router = Router();

// 통합 검색
router.get("/", searchController);
// 인기 검색어를 먼저 명시
router.get("/popular", popularSearchController);
// 최근 검색어 조회 - 로그인 필수
router.get("/recent", authMiddleware, recentSearchController);
// 최근 검색어 삭제
router.delete("/recent/:id", authMiddleware, deleteRecentSearchController);

export default router;
