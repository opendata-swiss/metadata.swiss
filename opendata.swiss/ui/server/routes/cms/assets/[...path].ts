const cmsAssets = process.env.NODE_ENV === 'production' ? 'cms-assets-remote' : 'cms-assets-local';

export default defineEventHandler(async (event) => {
  const storage = useStorage(cmsAssets);
  const path = getRouterParam(event, 'path');

  if (!path) {
    throw createError({
      statusCode: 404,
    });
  }

  const item = await storage.getItemRaw(path);
  if (!item) {
    throw createError({
      statusCode: 404,
    });
  }

  return item
});
