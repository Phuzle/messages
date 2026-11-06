package com.phuzle.labs.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuzle.labs.messages.domain.model.Category
import com.phuzle.labs.messages.domain.search.SearchDirection
import com.phuzle.labs.messages.domain.search.SearchFilters
import com.phuzle.labs.messages.domain.search.SearchRange
import com.phuzle.labs.messages.domain.search.SearchSort
import com.phuzle.labs.messages.ui.theme.MessagesTheme

/** Bottom sheet for narrowing and ordering search results. Every change applies immediately. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchFiltersSheet(
    visible: Boolean,
    filters: SearchFilters,
    categories: List<Category>,
    onChange: (SearchFilters) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    val tokens = MessagesTheme.tokens
    Box(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(tokens.overlayBg).clickable(onClick = onDismiss))
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .background(tokens.surface, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .navigationBarsPadding(),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 10.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .background(tokens.textTertiary.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
            )
            Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Search filters", color = tokens.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (filters.activeCount > 0) {
                    Box(
                        Modifier.heightIn(min = 48.dp).clickable { onChange(SearchFilters()) }.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Reset", color = tokens.accentLabel, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 20.dp)) {
                FilterGroup("Sort by") {
                    SearchSort.entries.forEach { PillButton(it.label, filters.sort == it, { onChange(filters.copy(sort = it)) }) }
                }
                if (categories.size > 1) {
                    FilterGroup("Category") {
                        categories.forEach { PillButton(it.label, filters.category == it, { onChange(filters.copy(category = it)) }) }
                    }
                }
                FilterGroup("Date") {
                    SearchRange.entries.forEach { PillButton(it.label, filters.range == it, { onChange(filters.copy(range = it)) }) }
                }
                FilterGroup("Direction") {
                    SearchDirection.entries.forEach { PillButton(it.label, filters.direction == it, { onChange(filters.copy(direction = it)) }) }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterGroup(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(top = 14.dp)) {
        SectionLabel(title, Modifier.padding(bottom = 4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) { content() }
    }
}
