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
const MAX_PAGES = 10; // Safety cap on Clist pages per fetch
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

        // Clist can return fewer rows than `limit` and put the rest on further pages,
        // so keep following `meta.next` until every upcoming contest is collected.
        const contests = [];
        const seenIds = new Set();
        let firstMeta = null;
        let url = 'https://clist.by/api/v4/contest/';
        let params = { limit: 500, order_by: 'start', start__gte: startGte };

        for (let page = 0; url && page < MAX_PAGES; page++) {
            const response = await axios.get(url, { headers: { 'Authorization': API_KEY }, params });
            const { meta, objects = [] } = response.data;
            if (!firstMeta) firstMeta = meta;

            for (const contest of objects) {
                if (!seenIds.has(contest.id)) {
                    seenIds.add(contest.id);
                    contests.push(contest);
                }
            }

            if (objects.length === 0 || !meta || !meta.next) break;
            url = new URL(meta.next, 'https://clist.by').toString(); // `next` already carries the query
            params = undefined;
        }

        contests.sort((a, b) => a.start.localeCompare(b.start));
        console.log(`Fetched ${contests.length} upcoming contests from Clist.`);

        // Update the cache
        cachedContests = { meta: { ...firstMeta, next: null, total_count: contests.length }, objects: contests };
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

// Privacy policy for the CPCT Android app (linked from the Google Play listing)
const PRIVACY_POLICY_HTML = `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>CPCT Privacy Policy</title>
<style>
  body { font-family: system-ui, -apple-system, "Segoe UI", Roboto, sans-serif; max-width: 720px;
         margin: 0 auto; padding: 32px 20px 64px; line-height: 1.6; color: #1a1a1a; background: #fff; }
  h1 { font-size: 1.8rem; margin-bottom: 0; }
  h2 { font-size: 1.15rem; margin-top: 2rem; }
  .muted { color: #5f6368; }
</style>
</head>
<body>
<h1>CPCT Privacy Policy</h1>
<p class="muted">CPCT (Competitive Programming Contest Tracker) &middot; Last updated: 19 October 2026</p>

<p>CPCT shows upcoming programming contests and can add them to your device calendar.
It is built to collect as little data as possible. This policy explains what the app does with information.</p>

<h2>No account, no personal data collected</h2>
<p>CPCT has no sign-up or login. We do not collect, store or sell your name, email address, contacts,
location or any other personal information, and the app contains no ads or analytics.</p>

<h2>Contest data</h2>
<p>To show contests, the app downloads a public list of upcoming contests from our server
(which gets it from <a href="https://clist.by">clist.by</a>). These requests contain no personal information.
Like any web server, our hosting provider (Vercel) may briefly keep standard technical logs such as
IP address and request time for security and reliability.</p>

<h2>Calendar permission</h2>
<p>If you allow calendar access, CPCT uses it only on your device to:</p>
<ul>
  <li>add contests to a calendar you can edit, and</li>
  <li>check which contests are already in your calendar, and show which calendar app and account they are saved in.</li>
</ul>
<p>Your calendar data, including your calendar account name or email, never leaves your device and is never sent to us
or anyone else. You can turn off auto-sync in the app, or revoke calendar access at any time in your device settings.</p>

<h2>Data stored on your device</h2>
<p>The app saves your chosen platforms, your auto-sync setting and a copy of the contest list on your device so it
works offline. This data stays on your device and is deleted when you uninstall the app or clear its storage.</p>

<h2>Children</h2>
<p>CPCT is not directed at children under 13 and does not knowingly collect information from anyone.</p>

<h2>Changes</h2>
<p>If this policy changes, the updated version will be posted on this page with a new date.</p>

<h2>Contact</h2>
<p>Questions about this policy: <a href="mailto:parvx18@gmail.com">parvx18@gmail.com</a></p>
</body>
</html>`;

app.get('/privacy', (req, res) => {
    res.type('html').send(PRIVACY_POLICY_HTML);
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
