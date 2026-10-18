package com.example.cpcontesttracker.ui

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.cpcontesttracker.R

/**
 * Supported platforms. [host] matches the "resource" field returned by the Clist API
 * and is also what gets stored in SharedPreferences.
 * Each platform has its own card colors: [cardColor] with [onCard] text and [onCardMuted] secondary text.
 */
enum class Platform(
    val host: String,
    val displayName: String,
    @DrawableRes val icon: Int,
    val cardColor: Color,
    val onCard: Color,
    val onCardMuted: Color
) {
    CODEFORCES("codeforces.com", "Codeforces", R.drawable.ic_codeforces, Color.White, Color(0xFF111111), Color(0xFF5F6368)),
    LEETCODE("leetcode.com", "LeetCode", R.drawable.ic_leetcode, Color(0xFFFFD54F), Color(0xFF1A1A1A), Color(0xFF6B5A1E)),
    CODECHEF("codechef.com", "CodeChef", R.drawable.ic_codechef, Color(0xFF8D6E63), Color.White, Color(0xFFE6D7D1)),
    ATCODER("atcoder.jp", "AtCoder", R.drawable.ic_atcoder, Color(0xFFBDBDBD), Color(0xFF111111), Color(0xFF4A4A4A)),
    GEEKSFORGEEKS("geeksforgeeks.org", "GeeksforGeeks", R.drawable.ic_generic_code, Color(0xFFA5D6A7), Color(0xFF111111), Color(0xFF3E5C40)),
    HACKERRANK("hackerrank.com", "HackerRank", R.drawable.ic_generic_code, Color(0xFFA5D6A7), Color(0xFF111111), Color(0xFF3E5C40)),
    HACKEREARTH("hackerearth.com", "HackerEarth", R.drawable.ic_generic_code, Color(0xFFB3BCF5), Color(0xFF111111), Color(0xFF414A80)),
    OTHER("", "Other", R.drawable.ic_generic_code, Color(0xFFA5D6A7), Color(0xFF111111), Color(0xFF3E5C40));

    companion object {
        val selectable = entries.filter { it != OTHER }

        fun from(resource: String): Platform = entries.firstOrNull { it.host == resource } ?: OTHER
    }
}
