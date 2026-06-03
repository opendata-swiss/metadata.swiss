import type { Comment, Page } from '#server/lib/webhooks/hyvor'
import Hyvor from '../../lib/webhooks/hyvor'
import type { HubSearch } from '../../lib/piveau'
import type { ListmonkConfig } from '../../lib/listmonk'
import Listmonk from '../../lib/listmonk'
import sinon from 'sinon'
import { expect } from 'chai'
import waitUntil from 'async-wait-until'

describe('Hyvor Webhooks', () => {
  describe('handleComment', () => {
    it('sends a notification when a top-level comment is created', async () => {
      // given
      const listmonk = {
        transactional: {
          send: sinon.stub().resolves({ ok: true }),
        },
      } as unknown as Listmonk
      const config = {
        publisherNotificationTemplateId: 4,
      }
      const piveau = {
        datasets: {
          get: sinon.stub().resolves({
            id: '123',
            title: {
              de: 'Test Dataset',
            },
            contact_point: [{
              name: 'Publisher',
              email: 'publisher@example.com',
            }],
          }),
        },
      } as unknown as HubSearch
      const hyvor = new Hyvor(config, listmonk, piveau)
      const comment: Comment = {
        parent: null,
        user: {
          email: 'commenter@example.com',
        },
        page: {
          identifier: 'dataset-123',
          url: 'https://example.com/dataset/123',
          title: 'Dataset',
        },
        body_html: '',
        status: 'published',
        history: [],
      }

      // when
      await hyvor.handleComment(comment)

      // then
      expect(listmonk.transactional.send).to.have.been.calledWith(sinon.match({
        template_id: 4,
        subscriber_email: 'publisher@example.com',
        subscriber_mode: 'external',
        data: {
          page: {
            url: 'https://example.com/dataset/123',
            title: 'Dataset',
          },
          publisher: {
            name: 'Publisher',
          },
          author: {
            email: 'commenter@example.com',
          },
        },
      }))
    })

    describe('using real Listmonk', function () {
      this.timeout(10000)

      before(function () {
        if (!process.env.LISTMONK_ADMIN_API_TOKEN) {
          this.skip()
        }
      })

      beforeEach(async () => {
        await fetch('http://localhost:8025/api/v1/messages', {
          method: 'DELETE',
        })
      })

      it('delivers email to mailpit', async () => {
        // given
        const listmonk = new Listmonk({
          api: {
            token: process.env.LISTMONK_ADMIN_API_TOKEN!,
            url: 'http://localhost:9000/',
            user: 'admin-api',
          },
        } as ListmonkConfig)
        const config = {
          publisherNotificationTemplateId: 5,
        }
        const piveau = {
          datasets: {
            get: sinon.stub().resolves({
              id: '123',
              title: {
                de: 'Test Dataset',
              },
              contact_point: [{
                name: 'Publisher',
                email: 'publisher@example.com',
              }],
            }),
          },
        } as unknown as HubSearch
        const hyvor = new Hyvor(config, listmonk, piveau)
        const comment: Comment = {
          parent: null,
          user: {
            email: 'commenter@example.com',
          },
          page: {
            identifier: 'dataset-123',
            url: 'https://example.com/dataset/123',
            title: 'Test Dataset',
          },
          body_html: '<p>Comment</p>',
          history: [],
          status: 'published',
        }

        // when
        const result = await hyvor.handleComment(comment)

        // then
        expect(result).to.be.undefined
        let data: { total: number, messages: unknown[] } | undefined
        await waitUntil(async () => {
          const mailpitRes = await fetch('http://localhost:8025/api/v1/messages?query=to:publisher@example.com')
          data = await mailpitRes.json()
          return data!.total === 1
        })

        await new Promise(resolve => setTimeout(resolve, 2000))
        expect(data).to.deep.contain({
          total: 1,
        })
        expect(data!.messages[0]).to.have.property('Subject', 'commentNotification')
      })
    })

    it('does not send a notification when comment is a reply', async () => {
      // given
      const listmonk = {
        transactional: {
          send: sinon.spy(),
        },
      } as unknown as Listmonk
      const config = {
        publisherNotificationTemplateId: 4,
      }
      const hyvor = new Hyvor(config, listmonk, {} as HubSearch)
      const comment: Comment = {
        parent: <Comment>{},
        page: <Page>{},
        user: {
          email: 'test@example.com',
        },
        body_html: '',
        status: 'published',
        history: [],
      }

      // when
      await hyvor.handleComment(comment)

      // then
      expect(listmonk.transactional.send).not.to.have.been.called
    })

    it('does not send a notification when created comment is pending', async () => {
      // given
      const listmonk = {
        transactional: {
          send: sinon.stub().resolves({ ok: true }),
        },
      } as unknown as Listmonk
      const config = {
        publisherNotificationTemplateId: 4,
      }
      const piveau = {
        datasets: {
          get: sinon.stub().resolves({
            id: '123',
            title: {
              de: 'Test Dataset',
            },
            contact_point: [{
              name: 'Publisher',
              email: 'publisher@example.com',
            }],
          }),
        },
      } as unknown as HubSearch
      const hyvor = new Hyvor(config, listmonk, piveau)
      const comment: Comment = {
        parent: null,
        page: {
          identifier: 'dataset-123',
          url: 'https://example.com/dataset/123',
          title: 'Dataset',
        },
        user: {
          email: 'test@example.com',
        },
        body_html: '',
        status: 'pending',
        history: [],
      }

      // when
      await hyvor.handleComment(comment)

      // then
      expect(listmonk.transactional.send).not.to.have.been.called
    })

    it('sends a notification when a pending was approved', async () => {
      // given
      const listmonk = {
        transactional: {
          send: sinon.stub().resolves({ ok: true }),
        },
      } as unknown as Listmonk
      const config = {
        publisherNotificationTemplateId: 4,
      }
      const piveau = {
        datasets: {
          get: sinon.stub().resolves({
            id: '123',
            title: {
              de: 'Test Dataset',
            },
            contact_point: [{
              name: 'Publisher',
              email: 'publisher@example.com',
            }],
          }),
        },
      } as unknown as HubSearch
      const hyvor = new Hyvor(config, listmonk, piveau)
      const comment: Comment = {
        parent: null,
        page: {
          identifier: 'dataset-123',
          url: 'https://example.com/dataset/123',
          title: 'Dataset',
        },
        user: {
          email: 'test@example.com',
        },
        body_html: '',
        status: 'published',
        history: [{
          type: 'moderation',
          new_status: 'published',
        }],
      }

      // when
      await hyvor.handleComment(comment)

      // then
      expect(listmonk.transactional.send).not.to.have.been.calledWith(sinon.match({
        template_id: 4,
        subscriber_email: 'publisher@example.com',
        subscriber_mode: 'external',
        data: {
          page: {
            url: 'https://example.com/dataset/123',
            title: 'Dataset',
          },
          publisher: {
            name: 'Publisher',
          },
          author: {
            email: 'commenter@example.com',
          },
        },
      }))
    })

    it('does not send a notification when template id is not configured', async () => {
      // given
      const listmonk = {
        transactional: {
          send: sinon.spy(),
        },
      } as unknown as Listmonk
      const config = {
        publisherNotificationTemplateId: 0,
      }
      const piveau = { } as unknown as HubSearch
      const hyvor = new Hyvor(config, listmonk, piveau)
      const comment: Comment = {
        parent: null,
        page: <Page>{},
        user: {
          email: 'test@example.com',
        },
        body_html: '',
        status: 'published',
        history: [],
      }

      // when
      await hyvor.handleComment(comment)

      // then
      expect(listmonk.transactional.send).not.to.have.been.called
    })
  })
})
