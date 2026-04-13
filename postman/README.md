Recommended import for the full video-style live flow:
[Delni_Live_Userflow.postman_collection.json](/Users/secrets/Desktop/Tuwaiq/dullani/postman/Delni_Live_Userflow.postman_collection.json)

Legacy happy-path seed flow:
[Delni.postman_collection.json](/Users/secrets/Desktop/Tuwaiq/dullani/postman/Delni.postman_collection.json)

Keep `baseUrl` as `http://localhost:8080`, then run the requests in order.

Flow:
1. Create city
2. Create user
3. Sync Google places
4. Load explore home
5. Refresh TikTok trends
6. Refresh one selected place trend
7. Generate trip
8. Get itinerary
9. Get place details
10. Save favorite
11. Get favorites
12. Replace first itinerary stop
13. Get itinerary again

Notes:
- The collection stores IDs automatically in collection variables.
- Start the backend from IntelliJ before testing.
- Make sure MySQL is running and matches [application.properties](/Users/secrets/Desktop/Tuwaiq/dullani/Delni/src/main/resources/application.properties).
- The live collection includes `POST /api/maps/sync-places` and `POST /api/trends/update`, so it depends on valid external API keys.
- For the new AI-assisted TikTok search flow, also add `OPENAI_API_KEY` in IntelliJ. Without it, the backend falls back to heuristic TikTok keyword generation.
- The trip request in the live collection auto-generates future dates so validation passes.
- The live collection now matches the DTO-first request bodies and the newer `/api/explore/home` response.
