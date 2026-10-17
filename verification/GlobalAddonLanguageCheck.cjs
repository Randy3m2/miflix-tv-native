'use strict';
const fs=require('fs'),assert=require('assert');
const root='app/src/main/java/com/miflix/native2/';
const read=p=>fs.readFileSync(root+p,'utf8');
const repo=read('data/StreamRepository.kt'),order=read('data/StreamOrdering.kt'),state=read('ui/AppState.kt');
assert(repo.includes('manifests.map { manifest ->'));
assert(repo.includes('}.awaitAll().flatten()'));
assert(repo.includes('movieTitle) },preferredAudio)'));
assert(order.indexOf('languageRank(it)')<order.indexOf('thenByDescending { quality(it) }'));
assert(order.indexOf('thenByDescending { quality(it) }')<order.indexOf('.distinctBy { it.url }'));
assert(state.includes('StreamOrdering.best(streams,audioLanguage)'));
assert.equal((state.match(/streamRepo.resolve\(imdb,item.type,season,episode,item.year,streamSizeBytes,item.title,audioLanguage\)/g)||[]).length,2);
assert(repo.includes('s.optString("description")'));
assert(repo.includes('languageValues(s.opt("audioLanguage"))'));
assert(order.includes('sorted(streams,preferredAudio).firstOrNull()'));
for(const rel of ['data/StreamRepository.kt','data/StreamOrdering.kt','data/StreamLanguagePriority.java','ui/AppState.kt','ui/UpdateNotes.kt'])
 assert.equal(read(rel),fs.readFileSync('main/java/com/miflix/native2/'+rel,'utf8'),rel);
console.log('PASS: all add-ons merged before global language ranking/dedup; automatic best uses audio setting; both source paths propagate preference; description/audio metadata recognized; mirrored sources.');
