const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  PageBreak, Header, Footer, PageNumber, NumberFormat,
  AlignmentType, HeadingLevel, WidthType, BorderStyle, ShadingType,
  TableLayoutType, LevelFormat, TableOfContents,
} = require("docx");
const fs = require("fs");

// ── Palette: GO-1 Graphite Orange ──
const P = {
  bg: "1A2330",
  primary: "0B1220",
  body: "000000",
  secondary: "506070",
  accent: "D4875A",
  surface: "F8F0EB",
  titleColor: "FFFFFF",
  subtitleColor: "B0B8C0",
  metaColor: "90989F",
  footerColor: "687078",
  table: {
    headerBg: "D4875A",
    headerText: "FFFFFF",
    accentLine: "D4875A",
    innerLine: "DDD0C8",
    surface: "F8F0EB",
  },
};

const NB = { style: BorderStyle.NONE, size: 0, color: "FFFFFF" };
const noBorders = { top: NB, bottom: NB, left: NB, right: NB };
const allNoBorders = { top: NB, bottom: NB, left: NB, right: NB, insideHorizontal: NB, insideVertical: NB };

function emptyPara() {
  return new Paragraph({ spacing: { line: 312 }, children: [new TextRun({ text: "", size: 2 })] });
}

// ── Helper: calcTitleLayout ──
function calcTitleLayout(title, maxWidthTwips, preferredPt = 40, minPt = 24) {
  const charWidth = (pt) => pt * 20;
  const charsPerLine = (pt) => Math.floor(maxWidthTwips / charWidth(pt));
  let titlePt = preferredPt;
  let lines;
  while (titlePt >= minPt) {
    const cpl = charsPerLine(titlePt);
    if (cpl < 2) { titlePt -= 2; continue; }
    lines = splitTitleLines(title, cpl);
    if (lines.length <= 3) break;
    titlePt -= 2;
  }
  if (!lines || lines.length > 3) {
    const cpl = charsPerLine(minPt);
    lines = splitTitleLines(title, cpl);
    titlePt = minPt;
  }
  return { titlePt, titleLines: lines };
}

function splitTitleLines(title, charsPerLine) {
  if (title.length <= charsPerLine) return [title];
  const breakAfter = new Set([...'\uFF0C\u3002\u3001\uFF1B\uFF1A\uFF01\uFF1F', ...'\u7684\u4E0E\u548C\u53CA\u4E4B\u5728\u4E8E\u4E3A', ...'-_\u2014\u2013\u00B7/', ...' \t']);
  const lines = [];
  let remaining = title;
  while (remaining.length > charsPerLine) {
    let breakAt = -1;
    for (let i = charsPerLine; i >= Math.floor(charsPerLine * 0.6); i--) {
      if (i < remaining.length && breakAfter.has(remaining[i - 1])) { breakAt = i; break; }
    }
    if (breakAt === -1) {
      const limit = Math.min(remaining.length, Math.ceil(charsPerLine * 1.3));
      for (let i = charsPerLine + 1; i < limit; i++) {
        if (breakAfter.has(remaining[i - 1])) { breakAt = i; break; }
      }
    }
    if (breakAt === -1) {
      breakAt = charsPerLine;
      const prev = remaining[breakAt - 1], next = remaining[breakAt];
      if (prev && next && !breakAfter.has(prev) && !breakAfter.has(next) &&
          /[\u4e00-\u9fff]/.test(prev) && /[\u4e00-\u9fff]/.test(next)) breakAt--;
    }
    lines.push(remaining.slice(0, breakAt).trim());
    remaining = remaining.slice(breakAt).trim();
  }
  if (remaining) lines.push(remaining);
  if (lines.length > 1 && lines[lines.length - 1].length <= 2) {
    const last = lines.pop();
    lines[lines.length - 1] += last;
  }
  return lines;
}

// ── Cover R4: Top Color Block ──
function buildCoverR4(config) {
  const padL = 1200, padR = 800;
  const availableWidth = 11906 - padL - padR;
  const { titlePt, titleLines } = calcTitleLayout(config.title, availableWidth, 40, 26);
  const titleSize = titlePt * 2;
  const titleBlockHeight = titleLines.length * (titlePt * 23 + 200);
  const englishLabelH = config.englishLabel ? (9 * 23 + 500) : 0;
  const subtitleH = config.subtitle ? (12 * 23 + 200) : 0;
  const upperContentH = englishLabelH + titleBlockHeight + subtitleH;
  const UPPER_MIN = 7500;
  const UPPER_H = Math.max(UPPER_MIN, upperContentH + 1500 + 800);
  const DIVIDER_H = 60;
  const contentEstimate = englishLabelH + titleBlockHeight + subtitleH;
  const spacerIntrinsic = 280;
  const topSpacing = Math.max(UPPER_H - contentEstimate - spacerIntrinsic - 800, 400);

  const upperBlock = new Table({
    width: { size: 100, type: WidthType.PERCENTAGE },
    layout: TableLayoutType.FIXED,
    borders: allNoBorders,
    rows: [new TableRow({
      height: { value: UPPER_H, rule: "exact" },
      children: [new TableCell({
        shading: { fill: P.bg }, borders: noBorders,
        verticalAlign: "top",
        margins: { left: padL, right: padR },
        children: [
          new Paragraph({ spacing: { before: topSpacing } }),
          config.englishLabel ? new Paragraph({
            spacing: { after: 500 },
            children: [new TextRun({ text: config.englishLabel.split("").join(" "),
              size: 18, color: P.accent, font: { ascii: "Calibri" }, characterSpacing: 60 })],
          }) : null,
          ...titleLines.map((line, i) => new Paragraph({
            spacing: { after: i < titleLines.length - 1 ? 100 : 200 },
            children: [new TextRun({ text: line, size: titleSize, bold: true,
              color: P.titleColor, font: { eastAsia: "SimHei", ascii: "Arial" } })],
          })),
          config.subtitle ? new Paragraph({
            spacing: { after: 100 },
            children: [new TextRun({ text: config.subtitle, size: 24, color: P.subtitleColor,
              font: { eastAsia: "Microsoft YaHei", ascii: "Arial" } })],
          }) : null,
        ].filter(Boolean),
      })],
    })],
  });

  const divider = new Table({
    width: { size: 100, type: WidthType.PERCENTAGE },
    borders: allNoBorders,
    rows: [new TableRow({
      height: { value: DIVIDER_H, rule: "exact" },
      children: [new TableCell({ borders: noBorders,
        shading: { fill: P.accent }, children: [emptyPara()] })],
    })],
  });

  const lowerContent = [
    new Paragraph({ spacing: { before: 800 } }),
    ...(config.metaLines || []).map(line => new Paragraph({
      indent: { left: padL }, spacing: { after: 100 },
      children: [new TextRun({ text: line, size: 28, color: P.metaColor,
        font: { eastAsia: "Microsoft YaHei", ascii: "Arial" } })],
    })),
    new Paragraph({ spacing: { before: 2000 } }),
    new Paragraph({
      indent: { left: padL },
      children: [
        new TextRun({ text: config.footerLeft || "", size: 22, color: "909090" }),
        new TextRun({ text: "          " }),
        new TextRun({ text: config.footerRight || "", size: 22, color: "909090" }),
      ],
    }),
  ];

  return [new Table({
    width: { size: 100, type: WidthType.PERCENTAGE },
    layout: TableLayoutType.FIXED,
    borders: allNoBorders,
    rows: [new TableRow({
      height: { value: 16838, rule: "exact" },
      children: [new TableCell({
        shading: { fill: "FFFFFF" }, borders: noBorders,
        verticalAlign: "top",
        children: [upperBlock, divider, ...lowerContent],
      })],
    })],
  })];
}

// ── Body component builders ──
function heading1(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_1,
    spacing: { before: 360, after: 160, line: 312 },
    children: [new TextRun({ text, bold: true, color: P.primary,
      font: { ascii: "Calibri", eastAsia: "SimHei" }, size: 32 })],
  });
}

function heading2(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_2,
    spacing: { before: 240, after: 120, line: 312 },
    children: [new TextRun({ text, bold: true, color: P.primary,
      font: { ascii: "Calibri", eastAsia: "SimHei" }, size: 28 })],
  });
}

function heading3(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_3,
    spacing: { before: 200, after: 100, line: 312 },
    children: [new TextRun({ text, bold: true, color: P.primary,
      font: { ascii: "Calibri", eastAsia: "SimHei" }, size: 24 })],
  });
}

function body(text) {
  return new Paragraph({
    alignment: AlignmentType.JUSTIFIED,
    indent: { firstLine: 480 },
    spacing: { line: 312 },
    children: [new TextRun({ text, size: 24, color: P.body,
      font: { ascii: "Calibri", eastAsia: "Microsoft YaHei" } })],
  });
}

function bodyNoIndent(text) {
  return new Paragraph({
    alignment: AlignmentType.LEFT,
    spacing: { line: 312 },
    children: [new TextRun({ text, size: 24, color: P.body,
      font: { ascii: "Calibri", eastAsia: "Microsoft YaHei" } })],
  });
}

function codeBlock(text) {
  return new Paragraph({
    spacing: { line: 280 },
    indent: { left: 480 },
    shading: { type: ShadingType.CLEAR, fill: "F5F5F0" },
    children: [new TextRun({ text, size: 21, font: { ascii: "Consolas", eastAsia: "Microsoft YaHei" }, color: "333333" })],
  });
}

// ── Table builder ──
function buildTable(headers, rows) {
  const colCount = headers.length;
  const colWidth = Math.floor(100 / colCount);
  return new Table({
    width: { size: 100, type: WidthType.PERCENTAGE },
    borders: {
      top: { style: BorderStyle.SINGLE, size: 2, color: P.table.accentLine },
      bottom: { style: BorderStyle.SINGLE, size: 2, color: P.table.accentLine },
      left: { style: BorderStyle.NONE },
      right: { style: BorderStyle.NONE },
      insideHorizontal: { style: BorderStyle.SINGLE, size: 1, color: P.table.innerLine },
      insideVertical: { style: BorderStyle.NONE },
    },
    rows: [
      new TableRow({
        tableHeader: true,
        cantSplit: true,
        children: headers.map(h => new TableCell({
          width: { size: colWidth, type: WidthType.PERCENTAGE },
          shading: { type: ShadingType.CLEAR, fill: P.table.headerBg },
          margins: { top: 60, bottom: 60, left: 120, right: 120 },
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            children: [new TextRun({ text: h, bold: true, size: 21, color: P.table.headerText,
              font: { ascii: "Calibri", eastAsia: "SimHei" } })],
          })],
        })),
      }),
      ...rows.map((row, idx) => new TableRow({
        cantSplit: true,
        children: row.map(cell => new TableCell({
          width: { size: colWidth, type: WidthType.PERCENTAGE },
          shading: { type: ShadingType.CLEAR, fill: idx % 2 === 0 ? "FFFFFF" : P.table.surface },
          margins: { top: 60, bottom: 60, left: 120, right: 120 },
          children: [new Paragraph({
            spacing: { line: 280 },
            children: [new TextRun({ text: cell, size: 21, color: P.body,
              font: { ascii: "Calibri", eastAsia: "Microsoft YaHei" } })],
          })],
        })),
      })),
    ],
  });
}

function tableTitle(text) {
  return new Paragraph({
    keepNext: true,
    spacing: { before: 200, after: 80, line: 312 },
    children: [new TextRun({ text, bold: true, size: 21, color: P.secondary,
      font: { ascii: "Calibri", eastAsia: "Microsoft YaHei" } })],
  });
}

// ── Build document ──
const doc = new Document({
  styles: {
    default: {
      document: {
        run: {
          font: { ascii: "Calibri", eastAsia: "Microsoft YaHei" },
          size: 24, color: P.body,
        },
        paragraph: { spacing: { line: 312 } },
      },
      heading1: {
        run: { font: { ascii: "Calibri", eastAsia: "SimHei" }, size: 32, bold: true, color: P.primary },
        paragraph: { spacing: { before: 360, after: 160, line: 312 } },
      },
      heading2: {
        run: { font: { ascii: "Calibri", eastAsia: "SimHei" }, size: 28, bold: true, color: P.primary },
        paragraph: { spacing: { before: 240, after: 120, line: 312 } },
      },
      heading3: {
        run: { font: { ascii: "Calibri", eastAsia: "SimHei" }, size: 24, bold: true, color: P.primary },
        paragraph: { spacing: { before: 200, after: 100, line: 312 } },
      },
    },
  },
  sections: [
    // ── Section 1: Cover ──
    {
      properties: {
        page: {
          size: { width: 11906, height: 16838 },
          margin: { top: 0, bottom: 0, left: 0, right: 0 },
        },
      },
      children: buildCoverR4({
        title: "Zerx \u9879\u76EE\u67B6\u6784\u8BBE\u8BA1\u6587\u6863",
        englishLabel: "ZERX FRAMEWORK DESIGN",
        subtitle: "Java \u591A\u6A21\u5757 Maven \u811A\u624B\u67B6\u9879\u76EE",
        metaLines: [
          "\u7248\u672C\uFF1Av1.0.0",
          "\u65E5\u671F\uFF1A2026-05-12",
          "\u4F5C\u8005\uFF1AForlor",
        ],
        footerLeft: "GitHub: github.com/forlor/zerx",
        footerRight: "JDK 21 | Maven | Spring Boot 3",
        palette: P,
      }),
    },

    // ── Section 2: TOC + Body ──
    {
      properties: {
        page: {
          size: { width: 11906, height: 16838 },
          margin: { top: 1440, bottom: 1440, left: 1701, right: 1417 },
          pageNumbers: { start: 1, formatType: NumberFormat.DECIMAL },
        },
      },
      headers: {
        default: new Header({
          children: [new Paragraph({
            alignment: AlignmentType.RIGHT,
            children: [new TextRun({ text: "Zerx \u9879\u76EE\u67B6\u6784\u8BBE\u8BA1\u6587\u6863", size: 18, color: "999999",
              font: { ascii: "Calibri", eastAsia: "Microsoft YaHei" } })],
          })],
        }),
      },
      footers: {
        default: new Footer({
          children: [new Paragraph({
            alignment: AlignmentType.CENTER,
            children: [new TextRun({ children: [PageNumber.CURRENT], size: 18, color: "999999" })],
          })],
        }),
      },
      children: [
        // TOC
        new TableOfContents("\u76EE\u5F55", {
          hyperlink: true,
          headingStyleRange: "1-3",
        }),
        new Paragraph({ spacing: { before: 200 }, children: [
          new TextRun({ text: "\u63D0\u793A\uFF1A\u5728 Word \u4E2D\u53F3\u952E\u76EE\u5F55\u9009\u62E9\u201C\u66F4\u65B0\u57DF\u201D\u53EF\u5237\u65B0\u9875\u7801", size: 18, color: "999999", italics: true,
            font: { ascii: "Calibri", eastAsia: "Microsoft YaHei" } }),
        ]}),
        new Paragraph({ children: [new PageBreak()] }),

        // ═══════════════════════════════════════
        // Chapter 1: Project Overview
        // ═══════════════════════════════════════
        heading1("1  \u9879\u76EE\u6982\u8FF0"),
        heading2("1.1  \u9879\u76EE\u80CC\u666F"),
        body("Zerx \u662F\u4E00\u4E2A\u9762\u5411\u4F01\u4E1A\u7EA7 Java \u5E94\u7528\u5F00\u53D1\u7684\u591A\u6A21\u5757 Maven \u811A\u624B\u67B6\u9879\u76EE\u3002\u5176\u6838\u5FC3\u8BBE\u8BA1\u7406\u5FF5\u662F\u201C\u5206\u5C42\u89E3\u8026\u3001\u6309\u9700\u53D6\u7528\u201D\uFF0C\u65E8\u5728\u4E3A\u5F00\u53D1\u56E2\u961F\u63D0\u4F9B\u4E00\u5957\u9AD8\u6548\u3001\u7075\u6D3B\u3001\u53EF\u7EF4\u62A4\u7684\u57FA\u7840\u8BBE\u65BD\u3002\u5728\u5F53\u524D\u5FEB\u901F\u8FED\u4EE3\u7684\u5F00\u53D1\u73AF\u5883\u4E2D\uFF0C\u56E2\u961F\u5F80\u5F80\u9700\u8981\u5728\u591A\u4E2A\u9879\u76EE\u4E4B\u95F4\u590D\u7528\u901A\u7528\u80FD\u529B\uFF0C\u540C\u65F6\u53C8\u5E0C\u671B\u907F\u514D\u5F15\u5165\u8FC7\u591A\u4E0D\u5FC5\u8981\u7684\u4F9D\u8D56\u3002Zerx \u7684\u8BBE\u8BA1\u6B63\u662F\u56E0\u6B64\u800C\u751F\uFF0C\u5B83\u5C06\u529F\u80FD\u62C6\u5206\u4E3A\u4E24\u5927\u90E8\u5206\uFF1A\u4F9D\u8D56\u6781\u5C11\u7684\u57FA\u7840\u811A\u624B\u67B6\u548C\u57FA\u4E8E Spring Boot 3 \u7684\u589E\u5F3A\u7EC4\u4EF6\u3002\u5F00\u53D1\u8005\u53EF\u4EE5\u6839\u636E\u9879\u76EE\u5B9E\u9645\u9700\u6C42\uFF0C\u7075\u6D3B\u5730\u7EC4\u5408\u6240\u9700\u6A21\u5757\uFF0C\u907F\u514D\u4E86\u201C\u4E00\u5200\u5207\u201D\u5F0F\u7684\u7B2C\u4E09\u65B9\u6846\u67B6\u5E26\u6765\u7684\u5305\u812F\u548C\u7EA6\u675F\u3002"),
        body("Zerx \u9879\u76EE\u91C7\u7528 JDK 21 \u4F5C\u4E3A\u57FA\u7840\u8FD0\u884C\u73AF\u5883\uFF0C\u5145\u5206\u5229\u7528 Java \u6700\u65B0\u7279\u6027\uFF0C\u5305\u62EC Record \u7C7B\u578B\u3001Pattern Matching\u3001Virtual Threads\u3001Sealed Classes \u7B49\uFF0C\u786E\u4FDD\u6846\u67B6\u5728\u6027\u80FD\u548C\u5F00\u53D1\u4F53\u9A8C\u4E0A\u59CB\u7EC8\u4FDD\u6301\u9886\u5148\u3002\u9879\u76EE\u4EE5 Maven \u4F5C\u4E3A\u6784\u5EFA\u5DE5\u5177\uFF0C\u91C7\u7528\u591A\u6A21\u5757\u7ED3\u6784\uFF0C\u901A\u8FC7\u660E\u786E\u7684\u6A21\u5757\u8FB9\u754C\u548C\u4F9D\u8D56\u5173\u7CFB\uFF0C\u5B9E\u73B0\u9AD8\u5EA6\u6A21\u5757\u5316\u7684\u5DE5\u7A0B\u5B9E\u8DF5\u3002"),

        heading2("1.2  \u8BBE\u8BA1\u76EE\u6807"),
        body("Zerx \u9879\u76EE\u7684\u8BBE\u8BA1\u9075\u5FAA\u4EE5\u4E0B\u6838\u5FC3\u76EE\u6807\uFF1A\u7B2C\u4E00\uFF0C\u6781\u7B80\u4F9D\u8D56\uFF0C\u57FA\u7840\u811A\u624B\u67B6\u90E8\u5206\u4EC5\u4F9D\u8D56 JDK \u6807\u51C6\u5E93\u548C\u5C11\u91CF\u7ECF\u8FC7\u4E25\u683C\u7B5B\u9009\u7684\u7B2C\u4E09\u65B9\u5E93\uFF0C\u786E\u4FDD\u6838\u5FC3\u80FD\u529B\u7684\u8F7B\u91CF\u5316\u548C\u7A33\u5B9A\u6027\uFF1B\u7B2C\u4E8C\uFF0C\u5206\u5C42\u67B6\u6784\uFF0C\u5C06\u57FA\u7840\u80FD\u529B\u4E0E\u6846\u67B6\u96C6\u6210\u80FD\u529B\u5206\u79BB\uFF0C\u4F7F\u5F97\u57FA\u7840\u6A21\u5757\u53EF\u4EE5\u72EC\u7ACB\u4E8E Spring Boot \u4F7F\u7528\uFF1B\u7B2C\u4E09\uFF0C\u9AD8\u5EA6\u53EF\u6269\u5C55\uFF0C\u6BCF\u4E2A\u6A21\u5757\u90FD\u63D0\u4F9B\u6E05\u6670\u7684\u6269\u5C55\u70B9\uFF0C\u652F\u6301\u5F00\u53D1\u8005\u6309\u9700\u5B9A\u5236\uFF1B\u7B2C\u56DB\uFF0C\u5F00\u7BB1\u5373\u7528\uFF0C\u63D0\u4F9B\u5408\u7406\u7684\u9ED8\u8BA4\u914D\u7F6E\uFF0C\u964D\u4F4E\u521D\u59CB\u5B66\u4E60\u6210\u672C\u3002"),

        heading2("1.3  \u6280\u672F\u6808\u6982\u89C8"),
        tableTitle("\u8868 1-1  \u6838\u5FC3\u6280\u672F\u6808"),
        buildTable(
          ["\u6280\u672F\u7EC4\u4EF6", "\u7248\u672C", "\u7528\u9014\u8BF4\u660E"],
          [
            ["JDK", "21 (LTS)", "\u8FD0\u884C\u73AF\u5883\uFF0C\u652F\u6301 Record\u3001Virtual Threads\u3001Pattern Matching \u7B49\u65B0\u7279\u6027"],
            ["Maven", "3.9+", "\u9879\u76EE\u6784\u5EFA\u4E0E\u4F9D\u8D56\u7BA1\u7406"],
            ["Spring Boot", "3.3+", "\u811A\u624B\u67B6\u7EC4\u4EF6\u57FA\u7840\u6846\u67B6"],
            ["JUnit 5", "5.10+", "\u5355\u5143\u6D4B\u8BD5\u6846\u67B6"],
            ["SLF4J", "2.0+", "\u65E5\u5FD7\u95E8\u9762\u62BD\u8C61"],
            [" Mockito", "5.x", "\u5355\u5143\u6D4B\u8BD5 Mock \u6846\u67B6"],
          ]
        ),

        // ═══════════════════════════════════════
        // Chapter 2: Project Architecture
        // ═══════════════════════════════════════
        heading1("2  \u9879\u76EE\u67B6\u6784\u8BBE\u8BA1"),
        heading2("2.1  \u6574\u4F53\u67B6\u6784"),
        body("Zerx \u9879\u76EE\u91C7\u7528\u591A\u6A21\u5757 Maven \u7ED3\u6784\uFF0C\u6574\u4F53\u67B6\u6784\u5206\u4E3A\u4E24\u5927\u90E8\u5206\uFF1Azerx-core\uFF08\u57FA\u7840\u811A\u624B\u67B6\uFF09\u548C zerx-spring\uFF08Spring Boot 3 \u811A\u624B\u67B6\u7EC4\u4EF6\uFF09\u3002\u6839\u9879\u76EE zerx-parent \u4F5C\u4E3A\u6700\u9876\u5C42\u7684\u7236 POM\uFF0C\u7EDF\u4E00\u7BA1\u7406\u6240\u6709\u5B50\u6A21\u5757\u7684\u4F9D\u8D56\u7248\u672C\u3001\u63D2\u4EF6\u914D\u7F6E\u548C\u6784\u5EFA\u53C2\u6570\u3002\u8FD9\u79CD\u5206\u5C42\u8BBE\u8BA1\u4F7F\u5F97\u57FA\u7840\u811A\u624B\u67B6\u5B8C\u5168\u72EC\u7ACB\u4E8E Spring \u751F\u6001\uFF0C\u53EF\u4EE5\u5728\u4EFB\u4F55 Java \u9879\u76EE\u4E2D\u5355\u72EC\u4F7F\u7528\uFF0C\u800C Spring Boot \u7EC4\u4EF6\u5219\u5728\u5176\u57FA\u7840\u4E0A\u63D0\u4F9B\u6846\u67B6\u96C6\u6210\u80FD\u529B\u3002"),
        body("\u9879\u76EE\u7684\u76EE\u5F55\u7ED3\u6784\u8BBE\u8BA1\u5982\u4E0B\uFF1A"),
        codeBlock("zerx/"),
        codeBlock("\u251C\u2500\u2500 pom.xml                          \u2190 \u7236 POM (zerx-parent)"),
        codeBlock("\u251C\u2500\u2500 zerx-core/                       \u2190 \u57FA\u7840\u811A\u624B\u67B6 (\u6781\u5C11\u4F9D\u8D56)"),
        codeBlock("\u2502   \u251C\u2500\u2500 zerx-common/                \u2190 \u901A\u7528\u5DE5\u5177\u7C7B"),
        codeBlock("\u2502   \u251C\u2500\u2500 zerx-exception/             \u2190 \u7EDF\u4E00\u5F02\u5E38\u5904\u7406"),
        codeBlock("\u2502   \u251C\u2500\u2500 zerx-logging/               \u2190 \u65E5\u5FD7\u62BD\u8C61"),
        codeBlock("\u2502   \u251C\u2500\u2500 zerx-crypto/                \u2190 \u52A0\u89E3\u5BC6\u5DE5\u5177"),
        codeBlock("\u2502   \u251C\u2500\u2500 zerx-http/                  \u2190 \u8F7B\u91CF HTTP \u5BA2\u6237\u7AEF"),
        codeBlock("\u2502   \u2514\u2500\u2500 zerx-core-bom/              \u2190 \u57FA\u7840\u6A21\u5757 BOM"),
        codeBlock("\u251C\u2500\u2500 zerx-spring/                      \u2190 Spring Boot 3 \u811A\u624B\u67B6"),
        codeBlock("\u2502   \u251C\u2500\u2500 zerx-spring-boot-starter/   \u2190 \u81EA\u52A8\u914D\u7F6E\u57FA\u5E95"),
        codeBlock("\u2502   \u251C\u2500\u2500 zerx-spring-web/            \u2190 Web \u5C42\u589E\u5F3A"),
        codeBlock("\u2502   \u251C\u2500\u2500 zerx-spring-data/           \u2190 \u6570\u636E\u8BBF\u95EE\u589E\u5F3A"),
        codeBlock("\u2502   \u251C\u2500\u2500 zerx-spring-security/       \u2190 \u5B89\u5168\u6846\u67B6\u96C6\u6210"),
        codeBlock("\u2502   \u251C\u2500\u2500 zerx-spring-cache/          \u2190 \u7F13\u5B58\u652F\u6301"),
        codeBlock("\u2502   \u251C\u2500\u2500 zerx-spring-mq/             \u2190 \u6D88\u606F\u961F\u5217\u652F\u6301"),
        codeBlock("\u2502   \u251C\u2500\u2500 zerx-spring-doc/            \u2190 API \u6587\u6863\u96C6\u6210"),
        codeBlock("\u2502   \u251C\u2500\u2500 zerx-spring-logging/        \u2190 \u8BF7\u6C42\u65E5\u5FD7"),
        codeBlock("\u2502   \u251C\u2500\u2500 zerx-spring-monitor/        \u2190 \u76D1\u63A7\u4E0E\u5065\u5EB7\u68C0\u67E5"),
        codeBlock("\u2502   \u2514\u2500\u2500 zerx-spring-bom/            \u2190 Spring \u6A21\u5757 BOM"),
        codeBlock("\u2514\u2500\u2500 docs/                            \u2190 \u8BBE\u8BA1\u6587\u6863"),

        heading2("2.2  \u6A21\u5757\u4F9D\u8D56\u5173\u7CFB"),
        body("\u5404\u6A21\u5757\u4E4B\u95F4\u7684\u4F9D\u8D56\u5173\u7CFB\u9075\u5FAA\u5355\u5411\u4F9D\u8D56\u539F\u5219\u3002zerx-common \u4F5C\u4E3A\u6700\u5E95\u5C42\u7684\u57FA\u7840\u6A21\u5757\uFF0C\u4E0D\u4F9D\u8D56\u4EFB\u4F55\u5176\u4ED6 Zerx \u6A21\u5757\uFF0C\u4EC5\u4F9D\u8D56 JDK \u6807\u51C6\u5E93\u3002\u5176\u4ED6\u57FA\u7840\u6A21\u5757\uFF08\u5982 zerx-exception\u3001zerx-logging \u7B49\uFF09\u5747\u53EF\u9009\u62E9\u6027\u4F9D\u8D56 zerx-common\u3002Spring Boot \u7EC4\u4EF6\u5C42\u7684\u6A21\u5757\u4F9D\u8D56 zerx-core \u4E2D\u7684\u57FA\u7840\u80FD\u529B\uFF0C\u5E76\u5728\u6B64\u57FA\u7840\u4E0A\u63D0\u4F9B Spring Boot \u81EA\u52A8\u914D\u7F6E\u548C\u96C6\u6210\u80FD\u529B\u3002\u6BCF\u4E2A Spring Boot Starter \u90FD\u662F\u72EC\u7ACB\u7684 JAR \u5305\uFF0C\u5F00\u53D1\u8005\u53EA\u9700\u5F15\u5165\u6240\u9700\u7684 Starter \u4F9D\u8D56\uFF0C\u5373\u53EF\u83B7\u5F97\u5BF9\u5E94\u529F\u80FD\uFF0C\u65E0\u9700\u5F15\u5165\u6574\u4E2A\u6846\u67B6\u3002"),

        // ═══════════════════════════════════════
        // Chapter 3: Core Scaffold
        // ═══════════════════════════════════════
        heading1("3  \u57FA\u7840\u811A\u624B\u67B6 (zerx-core)"),
        body("\u57FA\u7840\u811A\u624B\u67B6\u662F Zerx \u7684\u6838\u5FC3\u5E95\u5EA7\uFF0C\u8BBE\u8BA1\u539F\u5219\u662F\u201C\u96F6\u6846\u67B6\u4F9D\u8D56\u201D\uFF0C\u4EC5\u4F9D\u8D56 JDK \u6807\u51C6\u5E93\u548C\u6781\u5C11\u7684\u7B2C\u4E09\u65B9\u5E93\uFF0C\u786E\u4FDD\u5728\u6CA1\u6709\u4EFB\u4F55\u6846\u67B6\u7684\u73AF\u5883\u4E0B\u4E5F\u80FD\u8FD0\u884C\u3002\u8FD9\u90E8\u5206\u6A21\u5757\u63D0\u4F9B\u7684\u80FD\u529B\u662F\u6240\u6709 Java \u9879\u76EE\u90FD\u53EF\u80FD\u9700\u8981\u7684\u901A\u7528\u80FD\u529B\uFF0C\u5305\u62EC\u5DE5\u5177\u7C7B\u3001\u5F02\u5E38\u5904\u7406\u3001\u65E5\u5FD7\u3001\u52A0\u89E3\u5BC6\u3001HTTP \u5BA2\u6237\u7AEF\u7B49\u3002"),

        heading2("3.1  zerx-common\uFF08\u901A\u7528\u5DE5\u5177\u7C7B\uFF09"),
        body("zerx-common \u662F\u6240\u6709\u57FA\u7840\u6A21\u5757\u7684\u5E95\u5C42\u4F9D\u8D56\uFF0C\u63D0\u4F9B\u5E7F\u6CDB\u4F7F\u7528\u7684\u5DE5\u5177\u65B9\u6CD5\u548C\u57FA\u7840\u7C7B\u578B\u5B9A\u4E49\u3002\u8BE5\u6A21\u5757\u4E0D\u4F9D\u8D56\u4EFB\u4F55\u7B2C\u4E09\u65B9\u5E93\uFF0C\u4EC5\u4F7F\u7528 JDK \u6807\u51C6\u5E93\u3002\u4E3B\u8981\u5305\u542B\u4EE5\u4E0B\u529F\u80FD\u7EC4\u4EF6\uFF1A"),
        heading3("3.1.1  \u5B57\u7B26\u4E32\u5DE5\u5177 (StringUtil)"),
        body("\u63D0\u4F9B\u5B57\u7B26\u4E32\u5E38\u7528\u64CD\u4F5C\u7684\u5DE5\u5177\u7C7B\uFF0C\u5305\u62EC\u5224\u7A7A\u3001\u88C1\u526A\u3001\u9A7C\u5CF0\u8F6C\u6362\u3001\u5E8F\u5217\u5316/\u53CD\u5E8F\u5217\u5316\u3001\u6B63\u5219\u5339\u914D\u3001\u7F16\u7801\u8F6C\u6362\u7B49\u65B9\u6CD5\u3002\u6240\u6709\u65B9\u6CD5\u5747\u4E3A\u9759\u6001\u65B9\u6CD5\uFF0C\u7EBF\u7A0B\u5B89\u5168\uFF0C\u65B9\u4FBF\u5728\u4EFB\u4F55\u573A\u666F\u4E0B\u76F4\u63A5\u8C03\u7528\u3002"),
        heading3("3.1.2  \u96C6\u5408\u5DE5\u5177 (CollectionUtil)"),
        body("\u63D0\u4F9B\u96C6\u5408\u64CD\u4F5C\u7684\u5DE5\u5177\u7C7B\uFF0C\u5305\u62EC\u96C6\u5408\u5224\u7A7A\u3001\u5206\u7EC4\u3001\u53BB\u91CD\u3001\u8FC7\u6EE4\u3001\u6298\u53E0\u3001\u6392\u5E8F\u7B49\u65B9\u6CD5\u3002\u5145\u5206\u5229\u7528 JDK 21 Stream API \u7684\u7279\u6027\uFF0C\u63D0\u4F9B\u66F4\u4E3A\u7B80\u6D01\u7684\u94FE\u5F0F\u8C03\u7528\u98CE\u683C\u3002"),
        heading3("3.1.3  \u65E5\u671F\u65F6\u95F4\u5DE5\u5177 (DateUtil)"),
        body("\u57FA\u4E8E java.time \u5305\u7684\u65E5\u671F\u65F6\u95F4\u5DE5\u5177\u7C7B\uFF0C\u63D0\u4F9B\u65E5\u671F\u683C\u5F0F\u5316\u3001\u89E3\u6790\u3001\u8BA1\u7B97\u5DEE\u503C\u3001\u65F6\u533A\u8F6C\u6362\u7B49\u529F\u80FD\u3002\u5BF9\u5E38\u7528\u65E5\u671F\u683C\u5F0F\u5B9A\u4E49\u6807\u51C6\u5E38\u91CF\uFF0C\u7EDF\u4E00\u9879\u76EE\u4E2D\u7684\u65E5\u671F\u5904\u7406\u98CE\u683C\u3002"),
        heading3("3.1.4  \u53CD\u5C04\u5DE5\u5177 (ReflectUtil)"),
        body("\u63D0\u4F9B\u53CD\u5C04\u64CD\u4F5C\u7684\u5DE5\u5177\u7C7B\uFF0C\u5305\u62EC\u5B57\u6BB5\u8BBF\u95EE\u3001\u65B9\u6CD5\u8C03\u7528\u3001\u6CE8\u89E3\u89E3\u6790\u7B49\u529F\u80FD\u3002\u5229\u7528 JDK 21 \u7684\u53CD\u5C04\u589E\u5F3A\u7279\u6027\uFF0C\u63D0\u4F9B\u7C7B\u578B\u5B89\u5168\u7684\u8BBF\u95EE\u65B9\u5F0F\uFF0C\u51CF\u5C11\u53CD\u5C04\u8C03\u7528\u7684\u6837\u677F\u4EE3\u7801\u3002"),
        heading3("3.1.5  \u57FA\u7840\u7C7B\u578B\u5B9A\u4E49"),
        body("\u63D0\u4F9B\u7EDF\u4E00\u7684\u57FA\u7840\u7C7B\u578B\u5B9A\u4E49\uFF0C\u5305\u62EC\u901A\u7528\u54CD\u5E94\u7ED3\u6784\u4F53 Result<T>\u3001\u5206\u9875\u8BF7\u6C42\u53C2\u6570 PageRequest\u3001\u5206\u9875\u54CD\u5E94\u7ED3\u679C PageResult<T>\u3001\u63A5\u53E3\u54CD\u5E94\u7801\u679A\u4E3E ResponseCode \u7B49\u3002\u8FD9\u4E9B\u7C7B\u578B\u5747\u4F7F\u7528 JDK 21 \u7684 Record \u7279\u6027\u5B9A\u4E49\uFF0C\u786E\u4FDD\u4E0D\u53EF\u53D8\u6027\u548C\u7EBF\u7A0B\u5B89\u5168\u3002"),

        heading2("3.2  zerx-exception\uFF08\u7EDF\u4E00\u5F02\u5E38\u5904\u7406\uFF09"),
        body("zerx-exception \u63D0\u4F9B\u4E00\u5957\u5B8C\u6574\u7684\u5F02\u5E38\u5904\u7406\u4F53\u7CFB\uFF0C\u4EC5\u4F9D\u8D56 zerx-common\u3002\u8BE5\u6A21\u5757\u5B9A\u4E49\u4E86\u7EDF\u4E00\u7684\u5F02\u5E38\u5C42\u7EA7\u7ED3\u6784\u3001\u4E1A\u52A1\u5F02\u5E38\u57FA\u7C7B\u4EE5\u53CA\u5F02\u5E38\u5DE5\u5177\u65B9\u6CD5\u3002\u8BBE\u8BA1\u7406\u5FF5\u662F\u901A\u8FC7\u5F02\u5E38\u7801\u4F53\u7CFB\u5C06\u4E0D\u540C\u7C7B\u578B\u7684\u9519\u8BEF\u7EDF\u4E00\u5206\u7C7B\uFF0C\u65B9\u4FBF\u5168\u5C40\u5F02\u5E38\u5904\u7406\u548C\u9519\u8BEF\u8FFD\u8E2A\u3002\u4E3B\u8981\u5305\u542B\u4EE5\u4E0B\u5185\u5BB9\uFF1A"),
        heading3("3.2.1  \u5F02\u5E38\u57FA\u7C7B\u4F53\u7CFB"),
        body("\u5B9A\u4E49\u9876\u5C42\u62BD\u8C61\u5F02\u5E38\u57FA\u7C7B ZerxException\uFF0C\u4EE5\u53CA\u4E1A\u52A1\u5F02\u5E38 BusinessException\u3001\u53C2\u6570\u6821\u9A8C\u5F02\u5E38 ValidationException\u3001\u6743\u9650\u5F02\u5E38 AuthorizationException\u3001\u8D44\u6E90\u672A\u627E\u5230\u5F02\u5E38 NotFoundException \u7B49\u5B50\u7C7B\u3002\u6BCF\u4E2A\u5F02\u5E38\u5747\u643A\u5E26\u9519\u8BEF\u7801\u548C\u8BE6\u7EC6\u4FE1\u606F\uFF0C\u652F\u6301\u94FE\u5F0F\u5F02\u5E38\u539F\u56E0\u8BBE\u7F6E\u3002"),
        heading3("3.2.2  \u5F02\u5E38\u7801\u679A\u4E3E"),
        body("\u5B9A\u4E49\u7EDF\u4E00\u7684\u5F02\u5E38\u7801\u679A\u4E3E ErrorCode\uFF0C\u91C7\u7528\u5206\u6BB5\u7F16\u7801\u89C4\u5219\u3002\u4F8B\u5982 1xxxx \u4E3A\u7CFB\u7EDF\u7EA7\u5F02\u5E38\uFF0C2xxxx \u4E3A\u4E1A\u52A1\u903B\u8F91\u5F02\u5E38\uFF0C3xxxx \u4E3A\u53C2\u6570\u6821\u9A8C\u5F02\u5E38\uFF0C4xxxx \u4E3A\u6743\u9650\u76F8\u5173\u5F02\u5E38\u7B49\u3002\u6BCF\u4E2A\u5F02\u5E38\u7801\u5305\u542B\u6570\u503C\u7F16\u7801\u3001\u63CF\u8FF0\u4FE1\u606F\u548C HTTP \u72B6\u6001\u7801\u6620\u5C04\u3002"),

        heading2("3.3  zerx-logging\uFF08\u65E5\u5FD7\u62BD\u8C61\uFF09"),
        body("zerx-logging \u63D0\u4F9B\u7EDF\u4E00\u7684\u65E5\u5FD7\u62BD\u8C61\u5C42\uFF0C\u57FA\u4E8E SLF4J \u63A5\u53E3\u8BBE\u8BA1\uFF0C\u4EC5\u4F9D\u8D56 zerx-common \u548C SLF4J API\u3002\u8BE5\u6A21\u5757\u7684\u8BBE\u8BA1\u76EE\u7684\u662F\u63D0\u4F9B\u4E00\u5957\u6807\u51C6\u5316\u7684\u65E5\u5FD7\u5DE5\u5177\uFF0C\u4F7F\u5F97\u9879\u76EE\u4E2D\u7684\u65E5\u5FD7\u8F93\u51FA\u98CE\u683C\u7EDF\u4E00\u3001\u53EF\u63A7\u3002\u4E3B\u8981\u5305\u542B\u65E5\u5FD7\u5DE5\u5177\u7C7B LoggerUtil\uFF0C\u63D0\u4F9B\u7ED3\u6784\u5316\u65E5\u5FD7\u8BB0\u5F55\u3001\u65E5\u5FD7\u4E0A\u4E0B\u6587\u4F20\u9012\u3001\u65E5\u5FD7\u683C\u5F0F\u5316\u7B49\u529F\u80FD\uFF0C\u652F\u6301\u5728\u591A\u7EBF\u7A0B\u73AF\u5883\u4E0B\u81EA\u52A8\u6CE8\u5165\u8BF7\u6C42 ID\u3001\u7528\u6237\u4FE1\u606F\u7B49\u4E0A\u4E0B\u6587\u3002"),

        heading2("3.4  zerx-crypto\uFF08\u52A0\u89E3\u5BC6\u5DE5\u5177\uFF09"),
        body("zerx-crypto \u63D0\u4F9B\u5E38\u7528\u7684\u52A0\u89E3\u5BC6\u5DE5\u5177\uFF0C\u4EC5\u4F9D\u8D56 zerx-common\u3002\u8BE5\u6A21\u5757\u5C01\u88C5\u4E86\u5E38\u89C1\u7684\u52A0\u5BC6\u7B97\u6CD5\uFF0C\u5305\u62EC\u5BF9\u79F0\u52A0\u5BC6\uFF08AES\uFF09\u3001\u54C8\u5E0C\u7B97\u6CD5\uFF08MD5\u3001SHA-256\u3001SHA-512\uFF09\u3001Base64 \u7F16\u89E3\u7801\u3001HMAC \u7B7E\u540D\u9A8C\u8BC1\u7B49\u3002\u6240\u6709\u5B9E\u73B0\u5747\u4F7F\u7528 JDK \u5185\u7F6E\u7684 JCA\uFF08Java Cryptography Architecture\uFF09\uFF0C\u65E0\u9700\u989D\u5916\u4F9D\u8D56\u3002\u63D0\u4F9B\u7EDF\u4E00\u7684 CryptoUtil \u5DE5\u5177\u7C7B\u63A5\u53E3\uFF0C\u652F\u6301\u94FE\u5F0F\u8C03\u7528\uFF0C\u4FBF\u4E8E\u5728\u4E0D\u540C\u573A\u666F\u4E0B\u7075\u6D3B\u4F7F\u7528\u3002"),

        heading2("3.5  zerx-http\uFF08\u8F7B\u91CF HTTP \u5BA2\u6237\u7AEF\uFF09"),
        body("zerx-http \u63D0\u4F9B\u4E00\u4E2A\u8F7B\u91CF\u7EA7\u7684 HTTP \u5BA2\u6237\u7AEF\u5C01\u88C5\uFF0C\u57FA\u4E8E JDK 21 \u5185\u7F6E\u7684 HttpClient \u5B9E\u73B0\uFF0C\u4EC5\u4F9D\u8D56 zerx-common\u3002\u8BE5\u6A21\u5757\u63D0\u4F9B\u7EDF\u4E00\u7684 HTTP \u8BF7\u6C42\u63A5\u53E3\uFF0C\u652F\u6301 GET\u3001POST\u3001PUT\u3001DELETE \u7B49\u5E38\u89C1\u65B9\u6CD5\uFF0C\u652F\u6301 JSON \u8BF7\u6C42\u4F53\u5E8F\u5217\u5316\u548C\u54CD\u5E94\u4F53\u53CD\u5E8F\u5217\u5316\uFF0C\u652F\u6301\u8BF7\u6C42\u62E6\u622A\u5668\u3001\u8D85\u65F6\u8BBE\u7F6E\u3001\u81EA\u5B9A\u4E49\u8BF7\u6C42\u5934\u7B49\u529F\u80FD\u3002\u5229\u7528 JDK 21 HttpClient \u7684\u5F02\u6B65\u975E\u963B\u585E\u7279\u6027\uFF0C\u63D0\u4F9B\u9AD8\u6027\u80FD\u7684 HTTP \u8BF7\u6C42\u80FD\u529B\u3002"),

        heading2("3.6  zerx-core-bom\uFF08\u57FA\u7840\u6A21\u5757 BOM\uFF09"),
        body("zerx-core-bom \u662F\u57FA\u7840\u811A\u624B\u67B6\u6A21\u5757\u7684 Bill of Materials\uFF0C\u91C7\u7528 Maven BOM \u673A\u5236\u7EDF\u4E00\u7BA1\u7406\u6240\u6709\u57FA\u7840\u6A21\u5757\u7684\u7248\u672C\u53F7\u3002\u4F7F\u7528\u65B9\u53EA\u9700\u5728 pom.xml \u4E2D\u5F15\u5165\u8BE5 BOM\uFF0C\u5373\u53EF\u514D\u53BB\u6BCF\u4E2A\u6A21\u5757\u5355\u72EC\u6307\u5B9A\u7248\u672C\u53F7\u7684\u9EBB\u70E6\uFF0C\u786E\u4FDD\u6A21\u5757\u95F4\u7248\u672C\u4E00\u81F4\u6027\u3002"),

        // ═══════════════════════════════════════
        // Chapter 4: Spring Boot Components
        // ═══════════════════════════════════════
        heading1("4  Spring Boot \u811A\u624B\u67B6\u7EC4\u4EF6 (zerx-spring)"),
        body("Spring Boot \u811A\u624B\u67B6\u7EC4\u4EF6\u662F Zerx \u7684\u589E\u5F3A\u5C42\uFF0C\u57FA\u4E8E Spring Boot 3.3+ \u548C Spring Framework 6.x \u5F00\u53D1\u3002\u8FD9\u90E8\u5206\u6A21\u5757\u4F9D\u8D56 zerx-core \u4E2D\u7684\u57FA\u7840\u80FD\u529B\uFF0C\u5E76\u5728\u6B64\u57FA\u7840\u4E0A\u63D0\u4F9B\u81EA\u52A8\u914D\u7F6E\u3001\u6846\u67B6\u96C6\u6210\u548C\u7EA6\u5B9A\u4E8E\u7F16\u7A0B\u80FD\u529B\u3002\u6BCF\u4E2A Starter \u90FD\u9075\u5FAA Spring Boot \u7684\u81EA\u52A8\u914D\u7F6E\u89C4\u8303\uFF0C\u901A\u8FC7 spring.factories \u6216 AutoConfiguration.imports \u6587\u4EF6\u5B9E\u73B0\u81EA\u52A8\u88C5\u914D\u3002"),

        heading2("4.1  zerx-spring-boot-starter\uFF08\u81EA\u52A8\u914D\u7F6E\u57FA\u5E95\uFF09"),
        body("zerx-spring-boot-starter \u662F\u6240\u6709 Spring Boot \u7EC4\u4EF6\u7684\u57FA\u5E95\u6A21\u5757\uFF0C\u63D0\u4F9B\u901A\u7528\u7684\u81EA\u52A8\u914D\u7F6E\u57FA\u7840\u8BBE\u65BD\u3002\u5305\u62EC\u901A\u7528\u914D\u7F6E\u5C5E\u6027\u7ED1\u5B9A\u673A\u5236\u3001\u6761\u4EF6\u88C5\u914D\u6CE8\u89E3\u5904\u7406\u5668\u3001\u73AF\u5883\u611F\u77E5\u914D\u7F6E\u7B49\u57FA\u7840\u80FD\u529B\u3002\u8BE5\u6A21\u5757\u5B9A\u4E49\u4E86\u7EDF\u4E00\u7684\u914D\u7F6E\u524D\u7F00\u201Czerx.\u201D\uFF0C\u6240\u6709\u7EC4\u4EF6\u7684\u914D\u7F6E\u9879\u5747\u5728\u8BE5\u524D\u7F00\u4E0B\u8FDB\u884C\u7BA1\u7406\uFF0C\u907F\u514D\u4E0E\u5176\u4ED6\u7EC4\u4EF6\u7684\u914D\u7F6E\u51B2\u7A81\u3002\u540C\u65F6\u63D0\u4F9B\u6838\u5FC3\u914D\u7F6E\u5C5E\u6027\u7C7B ZerxProperties\uFF0C\u652F\u6301\u5F00\u53D1\u8005\u901A\u8FC7 application.yml \u8FDB\u884C\u5168\u5C40\u914D\u7F6E\u3002"),

        heading2("4.2  zerx-spring-web\uFF08Web \u5C42\u589E\u5F3A\uFF09"),
        body("zerx-spring-web \u63D0\u4F9B\u5BF9 Spring MVC \u7684\u589E\u5F3A\u80FD\u529B\u3002\u4E3B\u8981\u5305\u542B\u5168\u5C40\u5F02\u5E38\u5904\u7406\u5668\uFF0C\u81EA\u52A8\u5C06\u4E1A\u52A1\u5F02\u5E38\u8F6C\u6362\u4E3A\u7EDF\u4E00\u7684\u54CD\u5E94\u683C\u5F0F\uFF1B\u53C2\u6570\u6821\u9A8C\u589E\u5F3A\uFF0C\u57FA\u4E8E JSR-380 \u6CE8\u89E3\u63D0\u4F9B\u66F4\u53CB\u597D\u7684\u6821\u9A8C\u9519\u8BEF\u63D0\u793A\uFF1B\u7EDF\u4E00\u7684\u54CD\u5E94\u4F53\u5C01\u88C5 ResponseResult<T>\uFF0C\u786E\u4FDD\u6240\u6709 API \u8FD4\u56DE\u683C\u5F0F\u4E00\u81F4\uFF1B\u8BF7\u6C42\u65E5\u5FD7\u62E6\u622A\u5668\uFF0C\u81EA\u52A8\u8BB0\u5F55\u8BF7\u6C42\u548C\u54CD\u5E94\u7684\u5173\u952E\u4FE1\u606F\uFF1B\u4EE5\u53CA CORS \u8DE8\u57DF\u914D\u7F6E\u81EA\u52A8\u88C5\u914D\u3002\u5F00\u53D1\u8005\u5F15\u5165\u8BE5 Starter \u540E\uFF0C\u5373\u53EF\u83B7\u5F97\u4E0A\u8FF0\u6240\u6709 Web \u5C42\u589E\u5F3A\u80FD\u529B\u3002"),

        heading2("4.3  zerx-spring-data\uFF08\u6570\u636E\u8BBF\u95EE\u589E\u5F3A\uFF09"),
        body("zerx-spring-data \u63D0\u4F9B\u6570\u636E\u8BBF\u95EE\u5C42\u7684\u589E\u5F3A\u80FD\u529B\uFF0C\u652F\u6301 MyBatis\u3001MyBatis-Plus \u548C Spring Data JPA \u591A\u79CD ORM \u6846\u67B6\u7684\u96C6\u6210\u3002\u4E3B\u8981\u63D0\u4F9B\u901A\u7528\u5206\u9875\u67E5\u8BE2\u5C01\u88C5\uFF0C\u7EDF\u4E00\u4E0D\u540C ORM \u6846\u67B6\u7684\u5206\u9875\u63A5\u53E3\uFF1B\u591A\u6570\u636E\u6E90\u914D\u7F6E\u652F\u6301\uFF0C\u81EA\u52A8\u88C5\u914D\u52A8\u6001\u6570\u636E\u6E90\u5207\u6362\uFF1B\u6570\u636E\u6E90\u76D1\u63A7\u7EC4\u4EF6\uFF0C\u63D0\u4F9B\u8FDE\u63A5\u6C60\u72B6\u6001\u76D1\u63A7\u548C\u6162 SQL \u68C0\u6D4B\uFF1B\u4EE5\u53CA\u81EA\u52A8\u586B\u5145\u5B57\u6BB5\u3001\u903B\u8F91\u5220\u9664\u7B49\u901A\u7528\u529F\u80FD\u7684\u62BD\u8C61\u5C01\u88C5\u3002\u8FD9\u4E9B\u529F\u80FD\u901A\u8FC7\u81EA\u52A8\u914D\u7F6E\u5B9E\u73B0\uFF0C\u5F00\u53D1\u8005\u65E0\u9700\u624B\u52A8\u914D\u7F6E\u5373\u53EF\u4F7F\u7528\u3002"),

        heading2("4.4  zerx-spring-security\uFF08\u5B89\u5168\u6846\u67B6\u96C6\u6210\uFF09"),
        body("zerx-spring-security \u63D0\u4F9B\u57FA\u4E8E Spring Security 6.x \u7684\u5B89\u5168\u6846\u67B6\u96C6\u6210\u3002\u4E3B\u8981\u63D0\u4F9B\u7EDF\u4E00\u7684\u8BA4\u8BC1\u63A5\u53E3 AuthenticationProvider \u62BD\u8C61\uFF0C\u652F\u6301\u591A\u79CD\u8BA4\u8BC1\u65B9\u5F0F\uFF08\u7528\u540D\u5BC6\u7801\u3001JWT Token\u3001OAuth2 \u7B49\uFF09\u7684\u7075\u6D3B\u5207\u6362\uFF1B\u7EDF\u4E00\u7684\u6388\u6743\u6A21\u578B\uFF0C\u652F\u6301\u57FA\u4E8E\u89D2\u8272\uFF08RBAC\uFF09\u548C\u57FA\u4E8E\u6743\u9650\uFF08ABAC\uFF09\u7684\u8BBF\u95EE\u63A7\u5236\uFF1B\u5B89\u5168\u4E0A\u4E0B\u6587\u4F20\u64AD\uFF0C\u81EA\u52A8\u5C06\u7528\u6237\u4FE1\u606F\u3001\u89D2\u8272\u3001\u6743\u9650\u7B49\u4FE1\u606F\u7ED1\u5B9A\u5230\u5F53\u524D\u7EBF\u7A0B\uFF1B\u4EE5\u53CA\u5E38\u89C1\u5B89\u5168\u653B\u51FB\u7684\u9632\u62A4\uFF0C\u5305\u62EC CSRF\u3001XSS\u3001SQL \u6CE8\u5165\u7B49\u3002"),

        heading2("4.5  zerx-spring-cache\uFF08\u7F13\u5B58\u652F\u6301\uFF09"),
        body("zerx-spring-cache \u63D0\u4F9B\u7EDF\u4E00\u7684\u7F13\u5B58\u62BD\u8C61\u5C42\uFF0C\u57FA\u4E8E Spring Cache \u89C4\u8303\u5B9E\u73B0\u3002\u652F\u6301\u591A\u79CD\u7F13\u5B58\u5B9E\u73B0\uFF0C\u5305\u62EC\u672C\u5730\u7F13\u5B58\uFF08Caffeine\uFF09\u548C\u5206\u5E03\u5F0F\u7F13\u5B58\uFF08Redis\uFF09\u3002\u63D0\u4F9B\u7F13\u5B58\u6CE8\u89E3\u7684\u589E\u5F3A\u5B9A\u4E49\uFF0C\u652F\u6301\u81EA\u5B9A\u4E49\u7F13\u5B58\u8FC7\u671F\u7B56\u7565\u3001\u7F13\u5B58\u952E\u751F\u6210\u7B56\u7565\u3001\u7F13\u5B58\u51B2\u51FB\u7A7A\u7F3A\u7B49\u529F\u80FD\u3002\u901A\u8FC7\u81EA\u52A8\u914D\u7F6E\uFF0C\u5F00\u53D1\u8005\u53EA\u9700\u914D\u7F6E\u7F13\u5B58\u7C7B\u578B\u548C\u8FDE\u63A5\u53C2\u6570\uFF0C\u5373\u53EF\u5FEB\u901F\u96C6\u6210\u7F13\u5B58\u80FD\u529B\u3002"),

        heading2("4.6  zerx-spring-mq\uFF08\u6D88\u606F\u961F\u5217\u652F\u6301\uFF09"),
        body("zerx-spring-mq \u63D0\u4F9B\u6D88\u606F\u961F\u5217\u7684\u62BD\u8C61\u5C42\u548C\u96C6\u6210\u80FD\u529B\u3002\u652F\u6301\u591A\u79CD\u6D88\u606F\u4E2D\u95F4\u4EF6\uFF0C\u5305\u62EC RabbitMQ\u3001Kafka \u548C Redis Stream\u3002\u63D0\u4F9B\u7EDF\u4E00\u7684\u6D88\u606F\u53D1\u9001\u63A5\u53E3\uFF0C\u5C01\u88C5\u590D\u6742\u7684\u6D88\u606F\u53D1\u9001\u903B\u8F91\uFF1B\u6D88\u606F\u76D1\u542C\u5668\u6CE8\u89E3\uFF0C\u7B80\u5316\u6D88\u606F\u6D88\u8D39\u7684\u5B9A\u4E49\u65B9\u5F0F\uFF1B\u6D88\u606F\u53EF\u9760\u6027\u4FDD\u969C\uFF0C\u5305\u62EC\u6D88\u606F\u786E\u8BA4\u673A\u5236\u3001\u91CD\u8BD5\u7B56\u7565\u3001\u6B7B\u4FE1\u961F\u5217\u7B49\u529F\u80FD\u3002\u8BE5\u6A21\u5757\u91C7\u7528\u7B56\u7565\u6A21\u5F0F\u8BBE\u8BA1\uFF0C\u652F\u6301\u5728\u4E0D\u540C\u7684\u6D88\u606F\u4E2D\u95F4\u4EF6\u4E4B\u95F4\u5E73\u6ED1\u5207\u6362\u3002"),

        heading2("4.7  zerx-spring-doc\uFF08API \u6587\u6863\u96C6\u6210\uFF09"),
        body("zerx-spring-doc \u63D0\u4F9B\u57FA\u4E8E SpringDoc OpenAPI \u7684 API \u6587\u6863\u96C6\u6210\u3002\u81EA\u52A8\u751F\u6210 API \u6587\u6863\u548C\u4EA4\u4E92\u5F0F\u63A5\u53E3\u8C03\u8BD5\u9875\u9762\uFF0C\u652F\u6301 OpenAPI 3.0 \u89C4\u8303\u3002\u63D0\u4F9B\u7EDF\u4E00\u7684 API \u5206\u7EC4\u914D\u7F6E\uFF0C\u81EA\u52A8\u8BFB\u53D6\u63A7\u5236\u5668\u5C42\u7EA7\u7684\u6CE8\u89E3\u4FE1\u606F\u751F\u6210\u6587\u6863\uFF1B\u652F\u6301\u81EA\u5B9A\u4E49\u54CD\u5E94\u793A\u4F8B\u3001\u5168\u5C40\u8BA4\u8BC1\u914D\u7F6E\u7B49\u529F\u80FD\u3002\u5F00\u53D1\u8005\u65E0\u9700\u989D\u5916\u914D\u7F6E\uFF0C\u5F15\u5165 Starter \u540E\u5373\u53EF\u8BBF\u95EE Swagger UI \u9875\u9762\u3002"),

        heading2("4.8  zerx-spring-logging\uFF08\u8BF7\u6C42\u65E5\u5FD7\uFF09"),
        body("zerx-spring-logging \u63D0\u4F9B Web \u8BF7\u6C42\u7684\u5168\u94FE\u8DEF\u65E5\u5FD7\u8BB0\u5F55\u80FD\u529B\u3002\u901A\u8FC7 Filter \u548C Interceptor \u5B9E\u73B0\u8BF7\u6C42\u5165\u53E3\u548C\u51FA\u53E3\u7684\u65E5\u5FD7\u8BB0\u5F55\uFF0C\u5305\u62EC\u8BF7\u6C42 URL\u3001\u8BF7\u6C42\u65B9\u6CD5\u3001\u8BF7\u6C42\u5934\u3001\u54CD\u5E94\u72B6\u6001\u7801\u3001\u54CD\u5E94\u65F6\u95F4\u7B49\u5173\u952E\u4FE1\u606F\u3002\u652F\u6301\u654F\u611F\u53C2\u6570\u8131\u654F\uFF0C\u9632\u6B62\u5BC6\u7801\u7B49\u654F\u611F\u4FE1\u606F\u88AB\u8BB0\u5F55\u5230\u65E5\u5FD7\u4E2D\uFF1B\u652F\u6301\u81EA\u5B9A\u4E49\u65E5\u5FD7\u683C\u5F0F\u8F93\u51FA\uFF08JSON\u3001\u6587\u672C\u7B49\uFF09\uFF1B\u652F\u6301\u6162\u8BF7\u6C42\u8B66\u544A\uFF0C\u81EA\u52A8\u5BF9\u54CD\u5E94\u65F6\u95F4\u8D85\u8FC7\u9608\u503C\u7684\u8BF7\u6C42\u8FDB\u884C\u6807\u8BB0\u3002"),

        heading2("4.9  zerx-spring-monitor\uFF08\u76D1\u63A7\u4E0E\u5065\u5EB7\u68C0\u67E5\uFF09"),
        body("zerx-spring-monitor \u63D0\u4F9B\u57FA\u4E8E Spring Boot Actuator \u7684\u76D1\u63A7\u589E\u5F3A\u80FD\u529B\u3002\u81EA\u5B9A\u4E49\u5065\u5EB7\u68C0\u67E5\u6307\u6807\uFF0C\u652F\u6301\u6DFB\u52A0\u6570\u636E\u5E93\u8FDE\u63A5\u3001Redis \u8FDE\u63A5\u3001\u6D88\u606F\u961F\u5217\u7B49\u5916\u90E8\u4F9D\u8D56\u7684\u5065\u5EB7\u72B6\u6001\u68C0\u67E5\uFF1B\u5E94\u7528\u6307\u6807\u91C7\u96C6\uFF0C\u81EA\u52A8\u91C7\u96C6 JVM \u5185\u5B58\u3001GC\u3001\u7EBF\u7A0B\u6C60\u7B49\u5173\u952E\u6307\u6807\uFF1B\u652F\u6301\u591A\u79CD\u76D1\u63A7\u7AEF\u70B9\u7684\u96C6\u6210\uFF0C\u5305\u62EC Prometheus\u3001Grafana\u3001ELK \u7B49\u4E3B\u6D41\u76D1\u63A7\u65B9\u6848\u3002"),

        heading2("4.10  zerx-spring-bom\uFF08Spring \u6A21\u5757 BOM\uFF09"),
        body("zerx-spring-bom \u662F Spring Boot \u7EC4\u4EF6\u6A21\u5757\u7684 Bill of Materials\uFF0C\u7EDF\u4E00\u7BA1\u7406\u6240\u6709 Spring Boot \u7EC4\u4EF6\u7684\u7248\u672C\u53F7\u3002\u4F7F\u7528\u65B9\u53EA\u9700\u5728 pom.xml \u4E2D\u5F15\u5165\u8BE5 BOM\uFF0C\u5373\u53EF\u83B7\u5F97\u7ECF\u8FC7\u5145\u5206\u6D4B\u8BD5\u7684\u7248\u672C\u7EC4\u5408\uFF0C\u907F\u514D\u7248\u672C\u51B2\u7A81\u95EE\u9898\u3002\u8BE5 BOM \u540C\u65F6\u4F1A\u5F15\u5165 zerx-core-bom\uFF0C\u786E\u4FDD\u57FA\u7840\u6A21\u5757\u7248\u672C\u7684\u4E00\u81F4\u6027\u3002"),

        // ═══════════════════════════════════════
        // Chapter 5: Maven Configuration
        // ═══════════════════════════════════════
        heading1("5  Maven \u914D\u7F6E\u89C4\u8303"),
        heading2("5.1  \u7236 POM \u8BBE\u8BA1"),
        body("\u6839\u9879\u76EE zerx-parent \u4F5C\u4E3A\u6700\u9876\u5C42\u7684\u7236 POM\uFF0C\u91C7\u7528 pom \u6253\u5305\u65B9\u5F0F\uFF0C\u7EDF\u4E00\u7BA1\u7406\u4EE5\u4E0B\u5185\u5BB9\uFF1AJDK \u7248\u672C\u8BBE\u7F6E\u4E3A 21\uFF0C\u901A\u8FC7 maven.compiler.source \u548C maven.compiler.target \u5C5E\u6027\u914D\u7F6E\uFF1B\u4F9D\u8D56\u7248\u672C\u7EDF\u4E00\u7BA1\u7406\uFF0C\u901A\u8FC7 dependencyManagement \u6307\u5B9A\u6240\u6709\u7B2C\u4E09\u65B9\u4F9D\u8D56\u7684\u7248\u672C\u53F7\uFF1B\u63D2\u4EF6\u7EDF\u4E00\u914D\u7F6E\uFF0C\u5305\u62EC maven-compiler-plugin\u3001maven-source-plugin\u3001maven-javadoc-plugin\u3001maven-surefire-plugin \u7B49\u5E38\u7528\u63D2\u4EF6\uFF1B\u4EE3\u7801\u8D28\u91CF\u914D\u7F6E\uFF0C\u96C6\u6210 Checkstyle\u3001SpotBugs \u7B49\u9759\u6001\u4EE3\u7801\u68C0\u67E5\u5DE5\u5177\u3002"),

        heading2("5.2  \u7248\u672C\u53F7\u89C4\u8303"),
        body("Zerx \u9879\u76EE\u91C7\u7528\u8BED\u4E49\u5316\u7248\u672C\u53F7\u89C4\u8303\uFF0C\u4E3B\u7248\u672C\u53F7\u91C7\u7528 v\u524D\u7F00\uFF0C\u5982 v1.0.0\u3001v1.1.0\u3001v2.0.0 \u7B49\u3002\u7248\u672C\u53F7\u7BA1\u7406\u9075\u5FAA\u8BED\u4E49\u5316\u7248\u672C\u89C4\u8303\uFF0C\u4E3B\u7248\u672C\u53F7\u5BF9\u5E94\u4E0D\u517C\u5BB9\u7684 API \u53D8\u66F4\uFF0C\u6B21\u7248\u672C\u53F7\u5BF9\u5E94\u5411\u540E\u517C\u5BB9\u7684\u529F\u80FD\u65B0\u589E\uFF0C\u4FEE\u8BA2\u7248\u672C\u53F7\u5BF9\u5E94\u5411\u540E\u517C\u5BB9\u7684 Bug \u4FEE\u590D\u3002\u540C\u65F6\u5728\u7236 POM \u4E2D\u5B9A\u4E49 ${zerx.version} \u5C5E\u6027\uFF0C\u7EDF\u4E00\u7BA1\u7406\u6240\u6709\u5B50\u6A21\u5757\u7684\u7248\u672C\u53F7\u3002"),

        // ═══════════════════════════════════════
        // Chapter 6: Module Details
        // ═══════════════════════════════════════
        heading1("6  \u6A21\u5757\u8BE6\u7EC6\u4FE1\u606F"),
        body("\u4E0B\u8868\u5B8C\u6574\u5217\u51FA\u4E86 Zerx \u9879\u76EE\u6240\u6709\u6A21\u5757\u7684\u8BE6\u7EC6\u4FE1\u606F\uFF0C\u5305\u62EC\u6A21\u5757\u540D\u79F0\u3001\u6240\u5C5E\u5206\u7EC4\u3001\u4E3B\u8981\u4F9D\u8D56\u548C\u6838\u5FC3\u80FD\u529B\u63CF\u8FF0\u3002"),
        tableTitle("\u8868 6-1  \u6A21\u5757\u8BE6\u7EC6\u4FE1\u606F"),
        buildTable(
          ["\u6A21\u5757\u540D\u79F0", "\u5206\u7EC4", "\u4E3B\u8981\u4F9D\u8D56", "\u6838\u5FC3\u80FD\u529B"],
          [
            ["zerx-common", "core", "\u65E0\uFF08\u4EC5 JDK\uFF09", "\u5B57\u7B26\u4E32\u3001\u96C6\u5408\u3001\u65E5\u671F\u3001\u53CD\u5C04\u7B49\u5DE5\u5177\u7C7B"],
            ["zerx-exception", "core", "zerx-common", "\u7EDF\u4E00\u5F02\u5E38\u4F53\u7CFB\u3001\u9519\u8BEF\u7801\u679A\u4E3E"],
            ["zerx-logging", "core", "zerx-common, SLF4J", "\u65E5\u5FD7\u62BD\u8C61\u3001\u7ED3\u6784\u5316\u65E5\u5FD7"],
            ["zerx-crypto", "core", "zerx-common", "AES\u3001MD5\u3001SHA\u3001Base64\u3001HMAC"],
            ["zerx-http", "core", "zerx-common", "\u8F7B\u91CF HTTP \u5BA2\u6237\u7AEF\u5C01\u88C5"],
            ["zerx-core-bom", "core", "\u65E0", "\u57FA\u7840\u6A21\u5757\u7248\u672C\u7EDF\u4E00\u7BA1\u7406"],
            ["zerx-spring-boot-starter", "spring", "zerx-core \u6A21\u5757", "\u81EA\u52A8\u914D\u7F6E\u57FA\u5E95\u3001\u914D\u7F6E\u5C5E\u6027\u7ED1\u5B9A"],
            ["zerx-spring-web", "spring", "zerx-spring-boot-starter", "\u5168\u5C40\u5F02\u5E38\u5904\u7406\u3001\u53C2\u6570\u6821\u9A8C\u3001\u54CD\u5E94\u5C01\u88C5"],
            ["zerx-spring-data", "spring", "zerx-spring-boot-starter", "\u5206\u9875\u3001\u591A\u6570\u636E\u6E90\u3001\u81EA\u52A8\u586B\u5145"],
            ["zerx-spring-security", "spring", "zerx-spring-boot-starter", "\u8BA4\u8BC1\u6388\u6743\u3001RBAC\u3001JWT"],
            ["zerx-spring-cache", "spring", "zerx-spring-boot-starter", "Caffeine\u3001Redis \u7F13\u5B58\u62BD\u8C61"],
            ["zerx-spring-mq", "spring", "zerx-spring-boot-starter", "RabbitMQ\u3001Kafka\u3001Redis Stream"],
            ["zerx-spring-doc", "spring", "zerx-spring-boot-starter", "OpenAPI 3.0 \u6587\u6863\u81EA\u52A8\u751F\u6210"],
            ["zerx-spring-logging", "spring", "zerx-spring-boot-starter", "\u8BF7\u6C42\u54CD\u5E94\u65E5\u5FD7\u3001\u6162\u8BF7\u6C42\u8B66\u544A"],
            ["zerx-spring-monitor", "spring", "zerx-spring-boot-starter", "\u5065\u5EB7\u68C0\u67E5\u3001\u6307\u6807\u91C7\u96C6\u3001\u76D1\u63A7\u96C6\u6210"],
            ["zerx-spring-bom", "spring", "\u65E0", "Spring \u6A21\u5757\u7248\u672C\u7EDF\u4E00\u7BA1\u7406"],
          ]
        ),
      ],
    },
  ],
});

// ── Generate ──
Packer.toBuffer(doc).then(buf => {
  fs.writeFileSync("/home/z/my-project/forlor/zerx/docs/zerx-architecture-design.docx", buf);
  console.log("Document generated successfully: zerx-architecture-design.docx");
}).catch(err => {
  console.error("Error:", err);
});
