export type LinkItem = {
  url: string;
  filename: string;
  source: string;
  code?: string;
  icon?: string;
};

export type SimpleAttachment = {
  url?: string;
  alt?: string;
  mediaType?: string;
};

export type AttachmentLike = unknown;

export type ConvertAttachmentToSimple = (attachment: AttachmentLike) => SimpleAttachment | undefined;

export function applyAttachmentToLinkItem(
  item: LinkItem,
  attachment: AttachmentLike,
  convertToSimple: ConvertAttachmentToSimple,
  source = "附件",
  icon = "",
): boolean {
  const simpleAttachment = convertToSimple(attachment);
  if (!simpleAttachment?.url) {
    return false;
  }

  item.url = simpleAttachment.url;
  item.filename = simpleAttachment.alt || filenameFromUrl(simpleAttachment.url) || item.filename;
  item.source = source;
  item.icon = icon;
  item.code = "";
  return true;
}

function filenameFromUrl(url: string): string {
  const pathname = url.split("?")[0]?.split("#")[0] || "";
  const filename = pathname.substring(pathname.lastIndexOf("/") + 1);
  try {
    return decodeURIComponent(filename);
  } catch {
    return filename;
  }
}
