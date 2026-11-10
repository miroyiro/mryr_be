import { Router } from "express";
import { authMiddleware } from "../middlewares/auth.middleware.js";
import { adminMiddleware } from "../middlewares/admin.middleware.js";
import {
  updateAnimeStreamingController,
  createArtistController,
  updateArtistController,
  createArtistAlbumController,
  createArtistSongController,
  createAdminEventController,
  deleteAdminEventController,
  updateAdminEventController,
  getAdminFollowsController,
  matchAdminFollowController,
} from "../controllers/admin.controller.js";

const router = Router();

// 관리자 애니메이션 수동 등록 링크 수정
router.put(
  "/anime/:id/streaming",
  authMiddleware,
  adminMiddleware,
  updateAnimeStreamingController,
);
// 관리자 아티스트 등록
router.post(
  "/artists",
  authMiddleware,
  adminMiddleware,
  createArtistController,
);
// 관리자 아티스트 정보 수정
router.patch(
  "/artists/:id",
  authMiddleware,
  adminMiddleware,
  updateArtistController,
);
// 관리자 앨범 등록
router.post(
  "/artists/:id/albums",
  authMiddleware,
  adminMiddleware,
  createArtistAlbumController,
);
// 관리자 곡 등록
router.post(
  "/artists/:id/songs",
  authMiddleware,
  adminMiddleware,
  createArtistSongController,
);
// 관리자 이벤트 등록
router.post(
  "/events",
  authMiddleware,
  adminMiddleware,
  createAdminEventController,
);
// 관리자 이벤트 삭제
router.delete(
  "/events/:id",
  authMiddleware,
  adminMiddleware,
  deleteAdminEventController,
);
// 관리자 이벤트 수정
router.patch(
  "/events/:id",
  authMiddleware,
  adminMiddleware,
  updateAdminEventController,
);
// 관리자 팔로우 요청 조회
router.get(
  "/follows",
  authMiddleware,
  adminMiddleware,
  getAdminFollowsController,
);
// 관리자 팔로우 요청 승인/거절
router.patch(
  "/follows/:followType/:followId",
  authMiddleware,
  adminMiddleware,
  matchAdminFollowController,
);

export default router;
