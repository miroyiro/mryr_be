import {
  getArtistDetailController,
  getArtistAlbumsController,
  followArtistController,
  unfollowArtistController,
  getArtistSongsController,
} from "../controllers/artist.controller.js";
import { authMiddleware } from "../middlewares/auth.middleware.js";

// 아티스트 상세 조회
router.get("/:id", getArtistDetailController);
// 아티스트 앨범 조회
router.get("/:id/albums", getArtistAlbumsController);
// 아티스트 팔로우
router.post("/:id/follow", authMiddleware, followArtistController);
// 아티스트 언팔로우
router.delete("/:id/follow", authMiddleware, unfollowArtistController);
// 아티스트 곡 조회
router.get("/:id/songs", getArtistSongsController);
