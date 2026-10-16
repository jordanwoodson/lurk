package io.github.jordanwoodson.patches.rumble.search

import app.morphe.patcher.Fingerprint

// SearchViewModel's constructor. It takes the use cases that save a search, update its time and read the
// recent searches from the local `queries` table.
internal object SearchViewModelConstructorFingerprint : Fingerprint(
    definingClass = "Lcom/rumble/battles/search/presentation/searchScreen/SearchViewModel;",
    name = "<init>",
    strings = listOf("saveQueryUseCase", "updateQueryUseCase", "getRecentQueriesUseCase"),
)
