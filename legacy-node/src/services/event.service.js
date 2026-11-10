import {
  findEvents,
  findEventDetail,
} from "../repositories/events.repository.js";

export async function getEvents(filters) {
  return findEvents(filters);
}

export async function getEventDetail({ eventId, userId }) {
  return findEventDetail({ eventId, userId });
}
