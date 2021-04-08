export default class FilesRPC {

  constructor (dmx, http) {
    this.dmx = dmx
    this.http = http
  }

  // File System Representation

  getFolderTopic (repoPath) {
    return this.http.get(`/files/folder/${encodeURIComponent(repoPath)}`)
      .then(response => new this.dmx.Topic(response.data))
  }

  getChildFileTopic (folderId, repoPath) {
    return this.http.get(`/files/parent/${folderId}/file/${encodeURIComponent(repoPath)}`)
      .then(response => new this.dmx.RelatedTopic(response.data))
  }

  getChildFolderTopic (folderId, repoPath) {
    return this.http.get(`/files/parent/${folderId}/folder/${encodeURIComponent(repoPath)}`)
      .then(response => new this.dmx.RelatedTopic(response.data))
  }

  // File Repository

  getDirectoryListing (repoPath) {
    return this.http.get(`/files/${encodeURIComponent(repoPath)}`)
      .then(response => response.data)
  }

  openFile(fileTopicId) {
    this.http.get(`/files/open/${fileTopicId}`)
  }

  // File Content

  getFileContent (repoPath) {
    return this.http.get(this.filerepoURL(repoPath))
      .then(response => response.data)
  }

  filerepoURL (repoPath) {
    return '/filerepo/' + encodeURIComponent(repoPath)
  }
}
