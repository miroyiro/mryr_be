import { Router } from "express";
import {
  getAnimeListController,
  getAnimeDetailController,
  getAnimeEpisodesController,
  followAnimeController,
  unfollowAnimeController,
  createAnimeReviewController,
  getAnimeReviewsController,
  updateAnimeReviewController,
  deleteAnimeReviewController,
  getAnimeStreamingController,
} from "../controllers/anime.controller.js";
import {
  optionalAuthMiddleware,
  authMiddleware,
} from "../middlewares/auth.middleware.js";

const router = Router();

// 애니메이션 목록 조회
router.get("/", getAnimeListController);
// 상세 조회
router.get("/:id", optionalAuthMiddleware, getAnimeDetailController);
// 에피소드 목록 조회
router.get("/:id/episodes", getAnimeEpisodesController);
// 애니 팔로우
router.post("/:id/follow", authMiddleware, followAnimeController);
// 애니 팔로우 취소
router.delete("/:id/follow", authMiddleware, unfollowAnimeController);
// 애니 리뷰 작성
router.post("/:id/reviews", authMiddleware, createAnimeReviewController);
// 애니 리뷰 목록 조회
router.get("/:id/reviews", getAnimeReviewsController);
// 애니 리뷰 수정
router.patch(
  "/:id/reviews/:reviewId",
  authMiddleware,
  updateAnimeReviewController,
);
// 애니 리뷰 삭제
router.delete(
  "/:id/reviews/:reviewId",
  authMiddleware,
  deleteAnimeReviewController,
);
// 애니 스트리밍 정보 조회
router.get("/:id/streaming", getAnimeStreamingController);
export default router;
