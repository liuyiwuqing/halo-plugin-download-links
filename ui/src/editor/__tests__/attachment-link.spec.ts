import { describe, expect, it } from "vitest";
import {
  applyAttachmentToLinkItem,
  type AttachmentLike,
  type LinkItem,
  type SimpleAttachment,
} from "../attachment-link";

function convertToSimple(attachment: AttachmentLike): SimpleAttachment | undefined {
  if (typeof attachment === "string") {
    return { url: attachment };
  }
  if (!attachment || typeof attachment !== "object") {
    return undefined;
  }
  if ("url" in attachment) {
    return attachment as SimpleAttachment;
  }
  if ("spec" in attachment) {
    const haloAttachment = attachment as {
      spec?: { displayName?: string; mediaType?: string };
      status?: { permalink?: string };
    };
    return {
      url: haloAttachment.status?.permalink || "",
      alt: haloAttachment.spec?.displayName,
      mediaType: haloAttachment.spec?.mediaType,
    };
  }
  return undefined;
}

function createLinkItem(): LinkItem {
  return {
    url: "",
    filename: "",
    source: "附件",
    code: "abcd",
    icon: "",
  };
}

describe("applyAttachmentToLinkItem", () => {
  it("fills download url and filename from a Halo attachment", () => {
    const item = createLinkItem();

    const applied = applyAttachmentToLinkItem(
      item,
      {
        spec: {
          displayName: "示例附件.zip",
          mediaType: "application/zip",
        },
        status: {
          permalink: "https://example.com/upload/example.zip",
        },
      },
      convertToSimple,
    );

    expect(applied).toBe(true);
    expect(item.url).toBe("https://example.com/upload/example.zip");
    expect(item.filename).toBe("示例附件.zip");
    expect(item.source).toBe("附件");
    expect(item.code).toBe("");
  });

  it("uses configured source and icon when provided", () => {
    const item = createLinkItem();

    applyAttachmentToLinkItem(
      item,
      {
        url: "https://example.com/upload/example.zip",
        alt: "example.zip",
      },
      convertToSimple,
      "Halo 附件",
      "/plugins/download-links/assets/static/icon/attachment.svg",
    );

    expect(item.source).toBe("Halo 附件");
    expect(item.icon).toBe("/plugins/download-links/assets/static/icon/attachment.svg");
  });

  it("falls back to filename in url when attachment display name is empty", () => {
    const item = createLinkItem();

    const applied = applyAttachmentToLinkItem(
      item,
      {
        url: "https://example.com/upload/%E5%A4%87%E7%94%A8.zip?download=true",
      },
      convertToSimple,
    );

    expect(applied).toBe(true);
    expect(item.filename).toBe("备用.zip");
  });

  it("keeps raw filename when url filename is not valid percent-encoded text", () => {
    const item = createLinkItem();

    const applied = applyAttachmentToLinkItem(
      item,
      {
        url: "https://example.com/upload/%E0%A4%A.zip",
      },
      convertToSimple,
    );

    expect(applied).toBe(true);
    expect(item.filename).toBe("%E0%A4%A.zip");
  });

  it("does not mutate item when attachment has no url", () => {
    const item = createLinkItem();

    const applied = applyAttachmentToLinkItem(
      item,
      {
        spec: {
          displayName: "无链接附件.zip",
        },
        status: {},
      },
      convertToSimple,
    );

    expect(applied).toBe(false);
    expect(item).toEqual(createLinkItem());
  });
});
