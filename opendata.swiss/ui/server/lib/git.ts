import type {RestEndpointMethodTypes} from "@octokit/rest";
import {Octokit} from "@octokit/rest";
import {createAppAuth} from "@octokit/auth-app";

const BASE_PATH = 'opendata.swiss/ui'

function getAuth() {
  if (process.env.GITHUB_APP_ID) {
    return {
      appId: parseInt(process.env.GITHUB_APP_ID),
      privateKey: process.env.GITHUB_APP_PRIVATE_KEY!,
      installationId: process.env.GITHUB_APP_INSTALLATION_ID!
    }
  }

  return process.env.GITHUB_TOKEN!
}

export default function (slug: string) {
  let baseSha: string;
  const prBranch = `submitted-showcase/${slug}`;

  const owner = process.env.GITHUB_OWNER!
  const repo = process.env.GITHUB_REPO!
  const baseBranch = process.env.GITHUB_BASE_BRANCH!

  const auth = getAuth();
  const octokit = new Octokit({
    authStrategy: typeof auth === 'string' ? undefined : createAppAuth,
    auth
  })

  const tree: RestEndpointMethodTypes['git']['createTree']['parameters']['tree'] = []

  return {
    async prepare() {
      try {
      const gotBranch = await octokit.repos.getBranch({
        owner,
        repo,
        branch: baseBranch,
      });
      baseSha = gotBranch.data.commit.sha;

      await octokit.git.createRef({
        owner,
        repo,
        branch: baseBranch,
        ref: `refs/heads/${prBranch}`,
        sha: baseSha,
      });
      } catch (error: any) {
        console.error(error)
        if (error.status === 422) {
          return false
        } else {
          throw error
        }
      }

      return true
    },
    async writeFile(path: string, payload: string | Buffer) {
      const blobContents = Buffer.isBuffer(payload) ?
        {content: payload.toString('base64'), encoding: 'base64'} :
        {content: payload, encoding: 'utf-8'};

      const blob = await octokit.git.createBlob({
        owner,
        repo,
        ...blobContents,
      });

      tree.push({
        path: `${BASE_PATH}/${path}`,
        type: 'blob',
        mode: '100644',
        sha: blob.data.sha
      });
    },
    async finalize(): Promise<boolean> {
      try {
        const commitTree = await octokit.git.createTree({
          owner,
          repo,
          tree,
          base_tree: baseSha,
        })

        const newCommit = await octokit.git.createCommit({
          owner,
          repo,
          message: `Add new showcase: ${slug}`,
          tree: commitTree.data.sha,
          parents: [baseSha],
        })

        await octokit.git.updateRef({
          owner,
          repo,
          ref: `heads/${prBranch}`,
          sha: newCommit.data.sha,
        })

        await octokit.pulls.create({
          owner,
          repo,
          title: `New showcase submission: ${slug}`,
          head: prBranch,
          base: baseBranch,
        })
      } catch (error) {
        console.error(error)
        return false
      }

      return true
    },
    async rollback() {
      try {
        await octokit.git.deleteRef({
          owner,
          repo,
          ref: `heads/${prBranch}`,
        })
      } catch (e) {
        console.log(e)
      }
    }
  }
}
