import * as fs from 'node:fs/promises'

export default function (rootDir: string) {
  return {
    writeFile(path: string, contents: string | Buffer) {
      const localPath = path.replace('content', 'content/.local')
      return fs.writeFile(`${rootDir}/${localPath}`, contents)
    },

    async finalize() {
      return true
    },
  }
}
