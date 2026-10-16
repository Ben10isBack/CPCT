const express = require('express');
const axios = require('axios');
const cors = require('cors');

const app = express();
app.use(cors());

// In-memory cache variables
let cachedContests = null;
let lastFetchTime = 0;

// Configuration
const CACHE_DURATION_MS = 60 * 60 * 1000; // 1 hour in milliseconds
// Use environment variables for security!
const API_KEY = process.env.CLIST_API_KEY;

app.get('/api/contests', async (req, res) => {
    const now = Date.now();

    // Check if we have valid cached data
    if (cachedContests && (now - lastFetchTime < CACHE_DURATION_MS)) {
        console.log("Serving from cache...");
        return res.json(cachedContests);
    }

    console.log("Cache expired or empty. Fetching from Clist API...");
    try {
        // Calculate the current UTC time formatted for Clist (YYYY-MM-DDTHH:MM:SS)
        const date = new Date();
        const isoString = date.toISOString(); // e.g., "2026-10-16T09:30:00.000Z"
        const startGte = isoString.substring(0, 19); // Remove the ".000Z"

        // Fetch exactly like the Android app did
        const response = await axios.get('https://clist.by/api/v4/contest/', {
            headers: { 'Authorization': API_KEY },
            params: {
                limit: 500,
                order_by: 'start',
                start__gte: startGte
            }
        });

        // Update the cache
        cachedContests = response.data;
        lastFetchTime = now;

        // Send the response
        res.json(cachedContests);
    } catch (error) {
        console.error("Error fetching from Clist:", error.message);

        // If the fetch fails but we have old cached data, serve the old data as a fallback!
        if (cachedContests) {
            console.log("Serving stale cache due to Clist failure.");
            return res.json(cachedContests);
        }

        res.status(500).json({ error: "Failed to fetch contests from Clist" });
    }
});

// Vercel serverless functions require exporting the express app
module.exports = app;

// For local testing:
if (require.main === module) {
    const port = process.env.PORT || 3000;
    app.listen(port, () => {
        console.log(`Proxy server running locally on http://localhost:${port}/api/contests`);
    });
}
