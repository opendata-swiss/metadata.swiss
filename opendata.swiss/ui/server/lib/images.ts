import type {ShowcaseStorage} from "~~/server/lib/showcaseStorage";
import sharp from "sharp";

export function storage(store: Omit<ShowcaseStorage, 'writeImage'>): ShowcaseStorage {
  return {
    ...store,
    async writeImage(path: string, contents: Buffer) {
      const resized = sharp(contents)
        .rotate()
        .resize(900)
        .jpeg({ quality: 90 })

      return this.writeFile(path, await resized.toBuffer())
    }
  }
}
