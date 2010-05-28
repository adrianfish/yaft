function YaftPermissions(data) {

	for(var i=0,j=data.length;i<j;i++) {
		if('yaft.discussion.create' === data[i])
			this.discussionCreate = true;
		else if('yaft.discussion.deleteAny' === data[i])
			this.discussionDeleteAny = true;
		else if('yaft.discussion.deleteOwn' === data[i])
			this.discussionDeleteOwn = true;
		else if('yaft.forum.create' === data[i])
			this.forumCreate = true;
		else if('yaft.forum.deleteAny' === data[i])
			this.forumDeleteAny = true;
		else if('yaft.forum.deleteOwn' === data[i])
			this.forumDeleteOwn = true;
		else if('yaft.message.create' === data[i])
			this.messageCreate = true;
		else if('yaft.message.deleteAny' === data[i])
			this.messageDeleteAny = true;
		else if('yaft.message.deleteOwn' === data[i])
			this.messageDeleteOwn = true;
		else if('yaft.message.read' === data[i])
			this.messageRead = true;
		else if('yaft.modify.permissions' === data[i])
			this.modifyPermissions = true;
		else if('yaft.view.invisible' === data[i])
			this.viewInvisible = true;
	}
}
