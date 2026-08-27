import { GameTUI } from './GameTUI.js';
console.log('=== \u5341\u5b57\u519b\u4e4b\u738b TypeScript \u7248 ===');
console.log('1066 \u5e74\u8bfa\u66fc\u5f81\u670d\u6a21\u62df\u5668');
new GameTUI().run().catch(err => {
    console.error('\u8fd0\u884c\u9519\u8bef:', err);
    process.exit(1);
});
//# sourceMappingURL=Main.js.map