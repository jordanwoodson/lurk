// Lurk's changes to Kick's chat page (chat.mobile.kick.com), which Kick shows in a WebView on the stream screen
// and the channel's Chat tab. KickChat evaluates `(<this function>)(options)` when the page starts and again when
// it finishes loading, with the switches that are on:
//   ignoreClears  keep the messages when a moderator clears the chat
//   hide          CSS selectors of chat lines to hide (subs and gifts, hosts, reward redemptions)
//   sevenTv       show 7TV emotes. sevenTvChannel is the channel's slug and sevenTvUser its owner's Kick user ID,
//                 each null when the app hasn't seen them; without either only global emotes show.
// The page is served by Kick and can change at any time, so every step only touches what it finds and does
// nothing otherwise.
(function (options) {
    'use strict';
    // At page start the WebView can still be showing the previous document.
    if (location.host !== 'chat.mobile.kick.com') return 'not the chat page';
    if (window.lurkChat) return 'already applied';
    window.lurkChat = options;
    const applied = [];

    function whenReady(run) {
        const go = () => {
            const app = document.getElementById('app');
            if (app) run(app);
        };
        if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', go, {once: true});
        else go();
    }

    if (options.hide && options.hide.length) {
        whenReady(() => {
            const style = document.createElement('style');
            style.textContent = options.hide.join(',') + '{display:none!important}';
            (document.head || document.documentElement).appendChild(style);
        });
        applied.push('hide ' + options.hide.join(' '));
    }

    // The page clears chat by emptying the message list (`app.innerHTML = ""`); nothing else empties it outside
    // the KICKs gift preview, which KickChat leaves alone.
    if (options.ignoreClears) {
        whenReady((app) => {
            const html = Object.getOwnPropertyDescriptor(Element.prototype, 'innerHTML');
            if (!html || !html.set) return;
            Object.defineProperty(app, 'innerHTML', {
                configurable: true,
                get() {
                    return html.get.call(this);
                },
                set(value) {
                    if (value !== '') html.set.call(this, value);
                },
            });
        });
        applied.push('ignore clears');
    }

    if (options.sevenTv) {
        sevenTv(options.sevenTvChannel, options.sevenTvUser);
        applied.push('7tv ' + (options.sevenTvUser || options.sevenTvChannel || 'global only'));
    }

    // 7TV: a word of a message that is the name of one of the channel's 7TV emotes, or of a global one, becomes
    // that emote's image. Messages already on the page are rewritten once the sets load, later ones as they come.
    function sevenTv(slug, user) {
        const CDN = 'https://cdn.7tv.app/emote/';
        // Only the page's origin goes out with these requests, never its URL (which holds Kick's config).
        const get = (url) => fetch(url, {credentials: 'omit', referrerPolicy: 'no-referrer'})
            .then((response) => (response.ok ? response.json() : null))
            .catch(() => null);
        const emotes = (set) => (set && Array.isArray(set.emotes) ? set.emotes : []);
        // 7TV knows a channel by its owner's Kick user ID. The app's channel screens often only fetch the
        // channel's lighter info, without it; Kick's full channel response has it.
        const owner = user ? Promise.resolve(user) : slug
            ? get('https://kick.com/api/v2/channels/' + encodeURIComponent(slug)).then((c) => (c && c.user_id) || null)
            : Promise.resolve(null);
        Promise.all([
            owner.then((id) => (id ? get('https://7tv.io/v3/users/kick/' + encodeURIComponent(id)) : null))
                .then((u) => emotes(u && u.emote_set)),
            get('https://7tv.io/v3/emote-sets/global').then(emotes),
        ]).then(([channel, global]) => {
            const byName = new Map();
            // The channel's emotes win over global ones with the same name.
            for (const emote of channel.concat(global)) {
                if (emote && typeof emote.id === 'string' && typeof emote.name === 'string' && !byName.has(emote.name)) {
                    byName.set(emote.name, emote.id);
                }
            }
            window.lurkChat.sevenTvCount = [channel.length, global.length];
            if (byName.size) whenReady((app) => watch(app, byName));
        });

        function watch(app, byName) {
            const rewrite = (content) => {
                if (content.dataset.lurk7tv) return;
                content.dataset.lurk7tv = '1';
                // Kick builds a message's content from text nodes, its own emote images and link spans; only the
                // text nodes are read.
                for (const node of Array.from(content.childNodes)) {
                    if (node.nodeType !== Node.TEXT_NODE) continue;
                    const words = node.data.split(/(\s+)/);
                    if (!words.some((word) => byName.has(word))) continue;
                    const parts = document.createDocumentFragment();
                    let text = '';
                    for (const word of words) {
                        const id = byName.get(word);
                        if (!id) {
                            text += word;
                        } else {
                            if (text) parts.appendChild(document.createTextNode(text));
                            text = '';
                            const image = document.createElement('img');
                            image.src = CDN + encodeURIComponent(id) + '/2x.webp';
                            image.alt = word;
                            image.decoding = 'async';
                            image.loading = 'lazy';
                            image.referrerPolicy = 'no-referrer';
                            image.classList.add('emote');
                            parts.appendChild(image);
                        }
                    }
                    if (text) parts.appendChild(document.createTextNode(text));
                    node.replaceWith(parts);
                }
            };
            const scan = (root) => {
                if (root.matches && root.matches('.message-content')) rewrite(root);
                if (root.querySelectorAll) root.querySelectorAll('.message-content').forEach(rewrite);
            };
            new MutationObserver((records) => {
                for (const record of records) record.addedNodes.forEach(scan);
            }).observe(app, {childList: true, subtree: true});
            scan(app);
        }
    }

    return applied.join(', ') || 'nothing';
})
