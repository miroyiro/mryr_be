import {
  findArtistDetail,
  findArtistAlbums,
  createArtistFollow,
  deleteArtistFollow,
  findArtistSongs,
} from "../repositories/artist.repository.js";

export async function getArtistDetail({ artistId }) {
  return findArtistDetail({ artistId });
}

export async function getArtistAlbums({ artistId, page, limit }) {
  return findArtistAlbums({ artistId, page, limit });
}

export async function followArtist({ artistId, userId }) {
  return createArtistFollow({ artistId, userId });
}

export async function unfollowArtist({ artistId, userId }) {
  return deleteArtistFollow({ artistId, userId });
}

export async function getArtistSongs({ artistId, page, limit }) {
  return findArtistSongs({ artistId, page, limit });
}
