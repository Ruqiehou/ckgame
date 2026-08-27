export var Season;
(function (Season) {
    Season["SPRING"] = "SPRING";
    Season["SUMMER"] = "SUMMER";
    Season["AUTUMN"] = "AUTUMN";
    Season["WINTER"] = "WINTER";
})(Season || (Season = {}));
export function seasonZh(s) {
    switch (s) {
        case Season.SPRING: return '春';
        case Season.SUMMER: return '夏';
        case Season.AUTUMN: return '秋';
        case Season.WINTER: return '冬';
    }
}
//# sourceMappingURL=Season.js.map