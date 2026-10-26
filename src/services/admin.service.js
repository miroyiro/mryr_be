import {
  replaceManualAnimeStreaming,
  createArtist,
  updateArtist,
  createArtistAlbum,
  createArtistSong,
  createAdminEvent,
  deleteAdminEvent,
  updateAdminEvent,
  findAdminFollowRequests,
  matchAdminFollow,
} from "../repositories/admin.repository.js";

export async function updateAnimeStreaming({ animeId, platforms }) {
  return replaceManualAnimeStreaming({ animeId, platforms });
}

export async function registerArtist(artistData) {
  return createArtist(artistData);
}

export async function editArtist({ artistId, fields }) {
  return updateArtist({ artistId, fields });
}

export async function registerArtistAlbum(albumData) {
  return createArtistAlbum(albumData);
}

export async function registerArtistSong(songData) {
  return await createArtistSong(songData);
}

export async function registerAdminEvent({ eventData, adminId }) {
  return await createAdminEvent({
    eventData,
    adminId,
  });
}

export async function removeAdminEvent(eventId) {
  return await deleteAdminEvent(eventId);
}

export async function editAdminEvent({ eventId, fields }) {
  return await updateAdminEvent({ eventId, fields });
}

export async function getAdminFollowRequests(options) {
  return await findAdminFollowRequests(options);
}

export async function resolveAdminFollow({
  followType,
  followId,
  targetId,
  matchStatus,
  applySameInput,
}) {
  return await matchAdminFollow({
    followType,
    followId,
    targetId,
    matchStatus,
    applySameInput,
  });
}
