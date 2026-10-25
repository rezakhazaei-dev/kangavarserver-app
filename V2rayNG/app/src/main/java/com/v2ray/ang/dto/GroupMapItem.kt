package com.v2ray.ang.dto

import com.v2ray.ang.dto.entities.SubscriptionUsageInfo

data class GroupMapItem(
    var id: String,
    var remarks: String,
    val usageInfo: SubscriptionUsageInfo? = null,
    /** When the subscription was last updated, in epoch milliseconds; -1 when never. */
    val lastUpdated: Long = -1L,
)