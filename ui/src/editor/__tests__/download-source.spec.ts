import {describe, expect, it} from "vitest";
import {
  ATTACHMENT_ICON,
  ATTACHMENT_SOURCE,
  withBuiltinAttachmentPreset,
} from "../download-source";

describe("withBuiltinAttachmentPreset", () => {
  it("appends attachment as a builtin source", () => {
    const presets = withBuiltinAttachmentPreset([
      {label: "百度云网盘", value: "百度云网盘", icon: "/baidu.png"},
    ]);

    expect(presets).toEqual([
      {label: "百度云网盘", value: "百度云网盘", icon: "/baidu.png"},
      {label: ATTACHMENT_SOURCE, value: ATTACHMENT_SOURCE, icon: ATTACHMENT_ICON},
    ]);
  });

  it("ignores attachment source from settings and keeps the builtin one", () => {
    const presets = withBuiltinAttachmentPreset([
      {label: ATTACHMENT_SOURCE, value: ATTACHMENT_SOURCE, icon: "/user-changed.png"},
      {label: "夸克网盘", value: "夸克网盘", icon: "/quark.png"},
    ]);

    expect(presets).toEqual([
      {label: "夸克网盘", value: "夸克网盘", icon: "/quark.png"},
      {label: ATTACHMENT_SOURCE, value: ATTACHMENT_SOURCE, icon: ATTACHMENT_ICON},
    ]);
  });
});
