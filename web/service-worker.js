const CACHE_NAME = 'weddin-game-cache-v1';
const urlsToCache = [
  './',
  'game.js',
  'fonts/PressStart2P-Regular.ttf',
  'assets/background.png',
  'assets/player.png',
  'assets/obstacle_1.png',
  'assets/obstacle_2.png',
  'assets/obstacle_3.png',
  'assets/bonus_1.png',
  'assets/bonus_2.png',
  'assets/bonus_3.png',
  'assets/bonus_4.png',
  'assets/ic_star.webp'
];

self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => cache.addAll(urlsToCache))
  );
});

self.addEventListener('fetch', event => {
  event.respondWith(
    caches.match(event.request).then(response => {
      return response || fetch(event.request);
    })
  );
});
